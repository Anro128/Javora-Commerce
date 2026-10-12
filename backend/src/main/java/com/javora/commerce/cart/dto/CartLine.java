package com.javora.commerce.cart.dto;

import java.math.BigDecimal;

public record CartLine(
		Long variantId,
		Long productId,
		String productName,
		String productSlug,
		String variantName,
		String sku,
		String imageUrl,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal lineTotal,
		int weightGram,
		int available,
		boolean purchasable,
		String issue) {

	public static final String UNAVAILABLE = "UNAVAILABLE";
	public static final String OUT_OF_STOCK = "OUT_OF_STOCK";
	public static final String LOW_STOCK = "LOW_STOCK";

	public boolean hasIssue() {
		return issue != null;
	}

}
