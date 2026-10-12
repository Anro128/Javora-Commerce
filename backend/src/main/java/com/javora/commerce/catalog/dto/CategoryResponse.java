package com.javora.commerce.catalog.dto;

import com.javora.commerce.catalog.entity.Category;

public record CategoryResponse(
		Long id,
		Long parentId,
		String name,
		String slug,
		String description,
		int sortOrder,
		boolean active) {

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(
				category.getId(),
				category.getParentId(),
				category.getName(),
				category.getSlug(),
				category.getDescription(),
				category.getSortOrder(),
				category.isActive());
	}

}
