package com.javora.commerce.order.dto;

import com.javora.commerce.order.Order;
import com.javora.commerce.user.entity.Address;

public record ShippingAddress(
		String recipientName,
		String phone,
		String province,
		String city,
		String district,
		String postalCode,
		String street) {

	public static ShippingAddress from(Address address) {
		return new ShippingAddress(
				address.getRecipientName(), address.getPhone(), address.getProvince(), address.getCity(),
				address.getDistrict(), address.getPostalCode(), address.getStreet());
	}

	public static ShippingAddress from(Order order) {
		return new ShippingAddress(
				order.getRecipientName(), order.getPhone(), order.getProvince(), order.getCity(),
				order.getDistrict(), order.getPostalCode(), order.getStreet());
	}

}
