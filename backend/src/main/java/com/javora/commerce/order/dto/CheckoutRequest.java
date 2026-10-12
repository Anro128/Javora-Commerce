package com.javora.commerce.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
		@NotNull(message = "Shipping address is required")
		Long addressId,

		@NotBlank(message = "Courier is required")
		String courier,

		@NotBlank(message = "Shipping service is required")
		String service) {
}
