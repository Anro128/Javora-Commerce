package com.javora.commerce.user.dto;

import com.javora.commerce.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
		@Size(max = 50, message = "Label must be at most 50 characters")
		String label,

		@NotBlank(message = "Recipient name is required")
		@Size(max = 100, message = "Recipient name must be at most 100 characters")
		String recipientName,

		@NotBlank(message = "Phone number is required")
		@Pattern(regexp = ValidationMessages.PHONE_PATTERN, message = ValidationMessages.PHONE_MESSAGE)
		String phone,

		@NotBlank(message = "Province is required")
		@Size(max = 100, message = "Province must be at most 100 characters")
		String province,

		@NotBlank(message = "City is required")
		@Size(max = 100, message = "City must be at most 100 characters")
		String city,

		@NotBlank(message = "District is required")
		@Size(max = 100, message = "District must be at most 100 characters")
		String district,

		@NotBlank(message = "Postal code is required")
		@Pattern(regexp = "^[0-9]{5}$", message = "Postal code must contain 5 digits")
		String postalCode,

		@NotBlank(message = "Street address is required")
		@Size(max = 255, message = "Street address must be at most 255 characters")
		String street,

		boolean defaultAddress) {
}
