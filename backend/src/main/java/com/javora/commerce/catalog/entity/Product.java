package com.javora.commerce.catalog.entity;

import com.javora.commerce.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "products")
public class Product extends BaseEntity {

	@Column(name = "category_id", nullable = false)
	private Long categoryId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String slug;

	@Column(columnDefinition = "text")
	private String description;

	@Column(name = "base_price", nullable = false, precision = 15, scale = 2)
	private BigDecimal basePrice;

	@Column(name = "weight_gram", nullable = false)
	private int weightGram;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductStatus status = ProductStatus.DRAFT;

}
