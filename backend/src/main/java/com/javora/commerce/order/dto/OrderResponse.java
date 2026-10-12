package com.javora.commerce.order.dto;

import com.javora.commerce.order.Order;
import com.javora.commerce.order.OrderItem;
import com.javora.commerce.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
		String orderNumber,
		OrderStatus status,
		BigDecimal subtotal,
		BigDecimal shippingCost,
		BigDecimal discount,
		BigDecimal total,
		String courier,
		String courierService,
		ShippingAddress address,
		List<OrderItemResponse> items,
		Instant createdAt,
		Instant expiresAt,
		Instant cancelledAt) {

	public static OrderResponse from(Order order, List<OrderItem> items) {
		return new OrderResponse(
				order.getOrderNumber(), order.getStatus(), order.getSubtotal(), order.getShippingCost(),
				order.getDiscount(), order.getTotal(), order.getCourier(), order.getCourierService(),
				ShippingAddress.from(order), items.stream().map(OrderItemResponse::from).toList(),
				order.getCreatedAt(), order.getExpiresAt(), order.getCancelledAt());
	}

}
