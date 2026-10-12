package com.javora.commerce.inventory;

import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

	private static final int MAX_PAGE_SIZE = 100;

	private final InventoryService inventoryService;

	@GetMapping
	public ApiResponse<PageResponse<InventoryRowResponse>> list(
			@RequestParam(required = false) String q,
			@RequestParam(defaultValue = "false") boolean lowStock,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE));
		int threshold = inventoryService.lowStockThreshold();
		PageResponse<InventoryRowResponse> response = PageResponse.from(
				inventoryService.search(q, lowStock, pageable).map(row -> {
					int available = row.quantity() - row.reserved();
					return new InventoryRowResponse(
							row.variantId(), row.sku(), row.variantName(), row.productId(), row.productName(),
							row.quantity(), row.reserved(), available, available <= threshold);
				}));
		return ApiResponse.ok(response);
	}

	@PutMapping("/{variantId}")
	public ApiResponse<Void> setQuantity(@PathVariable Long variantId, @Valid @RequestBody StockUpdateRequest request) {
		inventoryService.setQuantity(variantId, request.quantity());
		return ApiResponse.ok("Stock updated", null);
	}

}
