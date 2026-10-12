package com.javora.commerce.order;

import com.javora.commerce.common.audit.BaseEntity;
import com.javora.commerce.common.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

	@Column(name = "order_number", nullable = false, updatable = false)
	private String orderNumber;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status = OrderStatus.PENDING_PAYMENT;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "shipping_cost", nullable = false, precision = 15, scale = 2)
	private BigDecimal shippingCost;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal discount = BigDecimal.ZERO;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal total;

	@Column(nullable = false)
	private String courier;

	@Column(name = "courier_service", nullable = false)
	private String courierService;

	@Column(name = "recipient_name", nullable = false)
	private String recipientName;

	@Column(nullable = false)
	private String phone;

	@Column(nullable = false)
	private String province;

	@Column(nullable = false)
	private String city;

	@Column(nullable = false)
	private String district;

	@Column(name = "postal_code", nullable = false)
	private String postalCode;

	@Column(nullable = false)
	private String street;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;

	public void transitionTo(OrderStatus next) {
		if (!status.canTransitionTo(next)) {
			throw new BusinessException(HttpStatus.CONFLICT,
					"Order cannot change from %s to %s".formatted(status, next));
		}
		this.status = next;
	}

}
