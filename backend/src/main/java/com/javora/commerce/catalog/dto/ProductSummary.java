package com.javora.commerce.catalog.dto;

import java.math.BigDecimal;

public record ProductSummary(
		Long id,
		String name,
		String slug,
		BigDecimal price,
		String imageUrl,
		String categoryName,
		String categorySlug,
		boolean inStock) {
}
