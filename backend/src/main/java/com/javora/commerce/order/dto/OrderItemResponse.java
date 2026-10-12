package com.javora.commerce.order.dto;

import com.javora.commerce.order.OrderItem;
import java.math.BigDecimal;

public record OrderItemResponse(
		Long variantId,
		Long productId,
		String productName,
		String variantName,
		String sku,
		BigDecimal unitPrice,
		int quantity,
		BigDecimal lineTotal) {

	public static OrderItemResponse from(OrderItem item) {
		return new OrderItemResponse(
				item.getVariantId(), item.getProductId(), item.getProductName(), item.getVariantName(),
				item.getSku(), item.getUnitPrice(), item.getQuantity(), item.getLineTotal());
	}

}
