package com.javora.commerce.catalog.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record VariantCreateRequest(
		@NotBlank(message = "SKU is required")
		@Pattern(regexp = "^[A-Za-z0-9._-]{1,64}$", message = "SKU may only contain letters, digits, dots, dashes, and underscores")
		String sku,

		@NotBlank(message = "Variant name is required")
		@Size(max = 100, message = "Variant name must be at most 100 characters")
		String name,

		@NotNull(message = "Price is required")
		@DecimalMin(value = "0.00", message = "Price must not be negative")
		@Digits(integer = 13, fraction = 2, message = "Price has too many digits")
		BigDecimal price,

		@NotNull(message = "Stock is required")
		@Min(value = 0, message = "Stock must not be negative")
		Integer stock) {
}
