package com.javora.commerce.user.dto;

import com.javora.commerce.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@NotBlank(message = "Full name is required")
		@Size(max = 100, message = "Full name must be at most 100 characters")
		String fullName,

		@Pattern(regexp = ValidationMessages.PHONE_PATTERN, message = ValidationMessages.PHONE_MESSAGE)
		String phone) {
}
