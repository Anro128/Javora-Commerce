package com.javora.commerce.auth.dto;

import com.javora.commerce.user.dto.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {

	public static AuthResponse bearer(String accessToken, long expiresIn, UserResponse user) {
		return new AuthResponse(accessToken, "Bearer", expiresIn, user);
	}

}
