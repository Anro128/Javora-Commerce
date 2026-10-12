package com.javora.commerce.catalog.dto;

import com.javora.commerce.catalog.dto.ProductContent.ImageView;
import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
		Long id,
		String name,
		String slug,
		String description,
		BigDecimal price,
		int weightGram,
		String categoryName,
		String categorySlug,
		List<ImageView> images,
		List<VariantResponse> variants) {

	public record VariantResponse(
			Long id, String sku, String name, BigDecimal price, int available, boolean inStock) {
	}

}
