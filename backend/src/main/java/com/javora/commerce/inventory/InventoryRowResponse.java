package com.javora.commerce.inventory;

public record InventoryRowResponse(
		Long variantId,
		String sku,
		String variantName,
		Long productId,
		String productName,
		int quantity,
		int reserved,
		int available,
		boolean lowStock) {
}
