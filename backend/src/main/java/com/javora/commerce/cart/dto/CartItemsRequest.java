package com.javora.commerce.cart.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CartItemsRequest(
		@NotNull(message = "Items are required")
		@Size(max = 100, message = "A cart can have at most 100 items")
		List<@Valid CartItemRequest> items) {
}
