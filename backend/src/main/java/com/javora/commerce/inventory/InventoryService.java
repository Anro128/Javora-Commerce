package com.javora.commerce.inventory;

import com.javora.commerce.common.exception.BusinessException;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.common.util.LikePattern;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

	private final InventoryRepository inventoryRepository;

	@Value("${app.catalog.low-stock-threshold:5}")
	private int lowStockThreshold;

	public int lowStockThreshold() {
		return lowStockThreshold;
	}

	@Transactional
	public void initialize(Long variantId, int quantity) {
		inventoryRepository.save(new Inventory(variantId, quantity));
	}

	@Transactional
	public void setQuantity(Long variantId, int quantity) {
		Inventory inventory = inventoryRepository.findById(variantId)
				.orElseThrow(() -> new ResourceNotFoundException("Inventory", variantId));
		if (quantity < inventory.getReservedQuantity()) {
			throw new BusinessException(HttpStatus.CONFLICT,
					"Quantity cannot be lower than the %d units reserved by pending orders"
							.formatted(inventory.getReservedQuantity()));
		}
		inventory.setQuantity(quantity);
	}

	@Transactional(readOnly = true)
	public Map<Long, Integer> availability(Collection<Long> variantIds) {
		Map<Long, Integer> result = new HashMap<>();
		if (variantIds.isEmpty()) {
			return result;
		}
		inventoryRepository.findByVariantIdIn(variantIds)
				.forEach(inventory -> result.put(inventory.getVariantId(), Math.max(0, inventory.available())));
		return result;
	}

	@Transactional(readOnly = true)
	public Map<Long, Inventory> byVariant(Collection<Long> variantIds) {
		Map<Long, Inventory> result = new HashMap<>();
		if (!variantIds.isEmpty()) {
			inventoryRepository.findByVariantIdIn(variantIds).forEach(inv -> result.put(inv.getVariantId(), inv));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public List<Long> productIdsInStock(Collection<Long> productIds) {
		return productIds.isEmpty() ? List.of() : inventoryRepository.findProductIdsInStock(productIds);
	}

	@Transactional(readOnly = true)
	public Map<Long, ProductStockTotals> totalsByProduct(Collection<Long> productIds) {
		Map<Long, ProductStockTotals> result = new HashMap<>();
		if (!productIds.isEmpty()) {
			inventoryRepository.stockTotals(productIds).forEach(totals -> result.put(totals.productId(), totals));
		}
		return result;
	}

	@Transactional(readOnly = true)
	public Page<InventoryRow> search(String query, boolean lowOnly, Pageable pageable) {
		return inventoryRepository.search(LikePattern.contains(query), lowOnly, lowStockThreshold, pageable);
	}

}
