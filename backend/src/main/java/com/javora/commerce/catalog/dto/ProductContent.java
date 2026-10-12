package com.javora.commerce.catalog.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

// Cacheable product data; stock is excluded because it changes far more often than content.
public record ProductContent(
		Long id,
		String name,
		String slug,
		String description,
		BigDecimal price,
		int weightGram,
		String categoryName,
		String categorySlug,
		List<ImageView> images,
		List<VariantContent> variants) implements Serializable {

	public record ImageView(Long id, String url) implements Serializable {
	}

	public record VariantContent(Long id, String sku, String name, BigDecimal price) implements Serializable {
	}

}
