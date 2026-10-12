package com.javora.commerce.user.dto;

import com.javora.commerce.user.entity.Role;
import com.javora.commerce.user.entity.User;
import java.time.Instant;

public record UserResponse(
		Long id,
		String email,
		String fullName,
		String phone,
		Role role,
		boolean emailVerified,
		Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getEmail(),
				user.getFullName(),
				user.getPhone(),
				user.getRole(),
				user.isEmailVerified(),
				user.getCreatedAt());
	}

}
