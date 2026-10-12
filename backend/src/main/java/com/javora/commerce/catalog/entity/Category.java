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
@Table(name = "categories")
public class Category extends BaseEntity {

	@Column(name = "parent_id")
	private Long parentId;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String slug;

	private String description;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(nullable = false)
	private boolean active = true;

}
