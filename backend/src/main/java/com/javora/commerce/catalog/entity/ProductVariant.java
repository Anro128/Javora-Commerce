package com.javora.commerce.catalog.entity;

import com.javora.commerce.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Column(nullable = false)
	private String sku;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal price;

	@Column(nullable = false)
	private boolean active = true;

}
