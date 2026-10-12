package com.javora.commerce.user.dto;

public record AddressResponse(
		Long id,
		String label,
		String recipientName,
		String phone,
		String province,
		String city,
		String district,
		String postalCode,
		String street,
		boolean defaultAddress) {
}
