package com.javora.commerce.order.dto;

import com.javora.commerce.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponse(
		String orderNumber,
		OrderStatus status,
		BigDecimal total,
		int itemCount,
		Instant createdAt) {
}
