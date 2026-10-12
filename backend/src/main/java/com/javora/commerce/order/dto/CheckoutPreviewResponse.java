package com.javora.commerce.order.dto;

import com.javora.commerce.cart.dto.CartLine;
import com.javora.commerce.shipping.ShippingOption;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutPreviewResponse(
		List<CartLine> items,
		BigDecimal subtotal,
		BigDecimal shippingCost,
		BigDecimal discount,
		BigDecimal total,
		int totalWeightGram,
		ShippingOption shipping,
		ShippingAddress address) {
}
