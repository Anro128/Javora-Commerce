package com.javora.commerce.catalog.entity;

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
@Table(name = "product_images")
public class ProductImage extends BaseEntity {

	@Column(name = "product_id", nullable = false, updatable = false)
	private Long productId;

	@Column(name = "object_key", nullable = false, updatable = false)
	private String objectKey;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

}
