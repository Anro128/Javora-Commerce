package com.javora.commerce.order;

public enum OrderStatus {
	PENDING_PAYMENT,
	PAID,
	PROCESSING,
	SHIPPED,
	DELIVERED,
	COMPLETED,
	CANCELLED,
	REFUNDED;

	// The single place that decides which status changes are legal.
	public boolean canTransitionTo(OrderStatus next) {
		return switch (this) {
			case PENDING_PAYMENT -> next == PAID || next == CANCELLED;
			case PAID -> next == PROCESSING || next == CANCELLED || next == REFUNDED;
			case PROCESSING -> next == SHIPPED || next == CANCELLED || next == REFUNDED;
			case SHIPPED -> next == DELIVERED;
			case DELIVERED -> next == COMPLETED || next == REFUNDED;
			case COMPLETED, CANCELLED, REFUNDED -> false;
		};
	}

}
