package com.javora.commerce.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 100, message = "Name must be at most 100 characters")
		String name,

		Long parentId,

		@Size(max = 500, message = "Description must be at most 500 characters")
		String description,

		@PositiveOrZero(message = "Sort order must not be negative")
		Integer sortOrder,

		Boolean active) {
}
