package com.javora.commerce.user.entity;

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
@Table(name = "addresses")
public class Address extends BaseEntity {

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	private String label;

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

	@Column(name = "is_default", nullable = false)
	private boolean defaultAddress;

}
