package com.javora.commerce.catalog.dto;

import com.javora.commerce.catalog.entity.ProductStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminProductResponse(
		Long id,
		Long categoryId,
		String name,
		String slug,
		String description,
		BigDecimal basePrice,
		int weightGram,
		ProductStatus status,
		List<AdminImageResponse> images,
		List<AdminVariantResponse> variants,
		Instant createdAt,
		Instant updatedAt) {

	public record AdminImageResponse(Long id, String url, int sortOrder) {
	}

	public record AdminVariantResponse(
			Long id, String sku, String name, BigDecimal price, boolean active, int quantity, int reserved) {
	}

}
