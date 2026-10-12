package com.javora.commerce.auth.dto;

import com.javora.commerce.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
		@NotBlank(message = "Token is required")
		String token,

		@NotBlank(message = "Password is required")
		@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
		@Pattern(regexp = ValidationMessages.PASSWORD_PATTERN, message = ValidationMessages.PASSWORD_MESSAGE)
		String newPassword) {
}
