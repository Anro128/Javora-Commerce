package com.javora.commerce.inventory;

public record InventoryRow(
		Long variantId,
		String sku,
		String variantName,
		Long productId,
		String productName,
		int quantity,
		int reserved) {
}
