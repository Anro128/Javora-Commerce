package com.javora.commerce.catalog.dto;

import com.javora.commerce.catalog.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record AdminProductSummary(
		Long id,
		String name,
		String slug,
		String categoryName,
		BigDecimal basePrice,
		ProductStatus status,
		long variantCount,
		long availableStock,
		String imageUrl,
		Instant createdAt) {
}
