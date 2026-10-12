package com.javora.commerce.cart;

import com.javora.commerce.cart.dto.CartLine;
import com.javora.commerce.catalog.entity.Product;
import com.javora.commerce.catalog.entity.ProductImage;
import com.javora.commerce.catalog.entity.ProductStatus;
import com.javora.commerce.catalog.entity.ProductVariant;
import com.javora.commerce.catalog.repository.ProductImageRepository;
import com.javora.commerce.catalog.repository.ProductRepository;
import com.javora.commerce.catalog.repository.ProductVariantRepository;
import com.javora.commerce.common.storage.StorageService;
import com.javora.commerce.inventory.InventoryService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Turns variant ids and quantities into priced lines using the current catalog and stock.
@Service
@RequiredArgsConstructor
public class CartLineAssembler {

	private final ProductVariantRepository variantRepository;
	private final ProductRepository productRepository;
	private final ProductImageRepository imageRepository;
	private final InventoryService inventoryService;
	private final StorageService storageService;

	// Variants that no longer exist are dropped; the input order is preserved.
	@Transactional(readOnly = true)
	public List<CartLine> assemble(Map<Long, Integer> quantities) {
		if (quantities.isEmpty()) {
			return List.of();
		}

		Map<Long, ProductVariant> variants = new HashMap<>();
		variantRepository.findAllById(quantities.keySet()).forEach(variant -> variants.put(variant.getId(), variant));

		List<Long> productIds = variants.values().stream().map(ProductVariant::getProductId).distinct().toList();
		Map<Long, Product> products = new HashMap<>();
		productRepository.findAllById(productIds).forEach(product -> products.put(product.getId(), product));

		Map<Long, String> images = new HashMap<>();
		for (ProductImage image : imageRepository.findByProductIdInOrderBySortOrderAscIdAsc(productIds)) {
			images.putIfAbsent(image.getProductId(), storageService.publicUrl(image.getObjectKey()));
		}
		Map<Long, Integer> availability = inventoryService.availability(variants.keySet());

		List<CartLine> lines = new ArrayList<>();
		quantities.forEach((variantId, quantity) -> {
			ProductVariant variant = variants.get(variantId);
			Product product = variant == null ? null : products.get(variant.getProductId());
			if (variant == null || product == null) {
				return;
			}

			int available = availability.getOrDefault(variantId, 0);
			boolean purchasable = variant.isActive() && product.getStatus() == ProductStatus.ACTIVE;
			lines.add(new CartLine(
					variantId,
					product.getId(),
					product.getName(),
					product.getSlug(),
					variant.getName(),
					variant.getSku(),
					images.get(product.getId()),
					variant.getPrice(),
					quantity,
					variant.getPrice().multiply(BigDecimal.valueOf(quantity)),
					product.getWeightGram(),
					available,
					purchasable,
					issueFor(purchasable, available, quantity)));
		});
		return lines;
	}

	private static String issueFor(boolean purchasable, int available, int quantity) {
		if (!purchasable) {
			return CartLine.UNAVAILABLE;
		}
		if (available <= 0) {
			return CartLine.OUT_OF_STOCK;
		}
		return quantity > available ? CartLine.LOW_STOCK : null;
	}

}
