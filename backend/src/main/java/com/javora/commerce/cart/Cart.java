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
@Table(name = "carts")
public class Cart extends BaseEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	public Cart(Long userId) {
		this.userId = userId;
	}

}
