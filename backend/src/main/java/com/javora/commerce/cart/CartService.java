package com.javora.commerce.cart;

import com.javora.commerce.cart.dto.CartItemRequest;
import com.javora.commerce.cart.dto.CartLine;
import com.javora.commerce.cart.dto.CartResponse;
import com.javora.commerce.cart.repository.CartItemRepository;
import com.javora.commerce.cart.repository.CartRepository;
import com.javora.commerce.catalog.entity.ProductStatus;
import com.javora.commerce.catalog.entity.ProductVariant;
import com.javora.commerce.catalog.repository.ProductRepository;
import com.javora.commerce.catalog.repository.ProductVariantRepository;
import com.javora.commerce.common.exception.BusinessException;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.inventory.InventoryService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

	static final int MAX_LINE_QUANTITY = 99;

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final ProductVariantRepository variantRepository;
	private final ProductRepository productRepository;
	private final InventoryService inventoryService;
	private final CartLineAssembler assembler;

	@Transactional
	public CartResponse getCart(Long userId) {
		return CartResponse.of(assembler.assemble(quantities(findOrCreateCart(userId))));
	}

	// Lines of the stored cart, used by checkout so it always works from server side data.
	@Transactional
	public List<CartLine> lines(Long userId) {
		return assembler.assemble(quantities(findOrCreateCart(userId)));
	}

	@Transactional(readOnly = true)
	public CartResponse preview(List<CartItemRequest> items) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		items.forEach(item -> quantities.merge(item.variantId(), item.quantity(), Integer::sum));
		quantities.replaceAll((variantId, quantity) -> Math.min(quantity, MAX_LINE_QUANTITY));
		return CartResponse.of(assembler.assemble(quantities));
	}

	@Transactional
	public CartResponse addItem(Long userId, Long variantId, int quantity) {
		Cart cart = findOrCreateCart(userId);
		int current = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
				.map(CartItem::getQuantity).orElse(0);
		setQuantity(cart, variantId, current + quantity);
		return CartResponse.of(assembler.assemble(quantities(cart)));
	}

	@Transactional
	public CartResponse updateItem(Long userId, Long variantId, int quantity) {
		Cart cart = findOrCreateCart(userId);
		cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
				.orElseThrow(() -> new ResourceNotFoundException("Cart item", variantId));
		setQuantity(cart, variantId, quantity);
		return CartResponse.of(assembler.assemble(quantities(cart)));
	}

	@Transactional
	public CartResponse removeItem(Long userId, Long variantId) {
		Cart cart = findOrCreateCart(userId);
		cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId).ifPresent(cartItemRepository::delete);
		return CartResponse.of(assembler.assemble(quantities(cart)));
	}

	@Transactional
	public void clear(Long userId) {
		cartRepository.findByUserId(userId).ifPresent(cart -> cartItemRepository.deleteAllByCartId(cart.getId()));
	}

	// Merging a guest cart never fails: unavailable items are skipped and quantities are capped to stock.
	@Transactional
	public CartResponse merge(Long userId, List<CartItemRequest> items) {
		Cart cart = findOrCreateCart(userId);
		for (CartItemRequest item : items) {
			ProductVariant variant = variantRepository.findById(item.variantId()).orElse(null);
			if (variant == null || !isPurchasable(variant)) {
				continue;
			}
			int available = inventoryService.availability(List.of(variant.getId())).getOrDefault(variant.getId(), 0);
			int current = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())
					.map(CartItem::getQuantity).orElse(0);
			int target = Math.min(Math.min(current + item.quantity(), available), MAX_LINE_QUANTITY);
			if (target > current) {
				upsert(cart, variant.getId(), target);
			}
		}
		return CartResponse.of(assembler.assemble(quantities(cart)));
	}

	private void setQuantity(Cart cart, Long variantId, int quantity) {
		ProductVariant variant = variantRepository.findById(variantId)
				.orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));
		if (!isPurchasable(variant)) {
			throw new BusinessException(HttpStatus.CONFLICT, "This product is no longer available");
		}
		if (quantity > MAX_LINE_QUANTITY) {
			throw new BusinessException("You can order at most %d units of one item".formatted(MAX_LINE_QUANTITY));
		}
		int available = inventoryService.availability(List.of(variantId)).getOrDefault(variantId, 0);
		if (quantity > available) {
			String message = available == 0
					? "This item is out of stock"
					: "Only %d available".formatted(available);
			throw new BusinessException(HttpStatus.CONFLICT, message);
		}
		upsert(cart, variantId, quantity);
	}

	private void upsert(Cart cart, Long variantId, int quantity) {
		CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
				.orElseGet(() -> new CartItem(cart.getId(), variantId, quantity));
		item.setQuantity(quantity);
		cartItemRepository.save(item);
	}

	private boolean isPurchasable(ProductVariant variant) {
		return variant.isActive() && productRepository.findById(variant.getProductId())
				.map(product -> product.getStatus() == ProductStatus.ACTIVE).orElse(false);
	}

	private Cart findOrCreateCart(Long userId) {
		return cartRepository.findByUserId(userId).orElseGet(() -> cartRepository.save(new Cart(userId)));
	}

	private Map<Long, Integer> quantities(Cart cart) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		cartItemRepository.findByCartIdOrderByIdAsc(cart.getId())
				.forEach(item -> quantities.put(item.getVariantId(), item.getQuantity()));
		return quantities;
	}

}
