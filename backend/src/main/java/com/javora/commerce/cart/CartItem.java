package com.javora.commerce.cart;

import com.javora.commerce.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

	@Column(name = "cart_id", nullable = false, updatable = false)
	private Long cartId;

	@Column(name = "variant_id", nullable = false, updatable = false)
	private Long variantId;

	@Column(nullable = false)
	private int quantity;

	public CartItem(Long cartId, Long variantId, int quantity) {
		this.cartId = cartId;
		this.variantId = variantId;
		this.quantity = quantity;
	}

}
