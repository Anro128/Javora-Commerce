package com.javora.commerce.order;

import com.javora.commerce.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// A snapshot of what was bought, so later catalog edits never change past orders.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

	@Column(name = "order_id", nullable = false, updatable = false)
	private Long orderId;

	@Column(name = "variant_id", nullable = false, updatable = false)
	private Long variantId;

	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Column(name = "product_name", nullable = false, updatable = false)
	private String productName;

	@Column(name = "variant_name", nullable = false, updatable = false)
	private String variantName;

	@Column(nullable = false, updatable = false)
	private String sku;

	@Column(name = "unit_price", nullable = false, updatable = false, precision = 15, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, updatable = false)
	private int quantity;

	@Column(name = "weight_gram", nullable = false, updatable = false)
	private int weightGram;

	@Column(name = "line_total", nullable = false, updatable = false, precision = 15, scale = 2)
	private BigDecimal lineTotal;

}
