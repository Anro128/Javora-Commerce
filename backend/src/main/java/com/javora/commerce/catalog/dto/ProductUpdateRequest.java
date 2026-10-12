package com.javora.commerce.catalog.dto;

import com.javora.commerce.catalog.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductUpdateRequest(
		@NotNull(message = "Category is required")
		Long categoryId,

		@NotBlank(message = "Name is required")
		@Size(max = 200, message = "Name must be at most 200 characters")
		String name,

		@Size(max = 10000, message = "Description must be at most 10000 characters")
		String description,

		@NotNull(message = "Base price is required")
		@DecimalMin(value = "0.00", message = "Base price must not be negative")
		@Digits(integer = 13, fraction = 2, message = "Base price has too many digits")
		BigDecimal basePrice,

		@NotNull(message = "Weight is required")
		@Positive(message = "Weight must be greater than zero")
		Integer weightGram,

		@NotNull(message = "Status is required")
		ProductStatus status) {
}
