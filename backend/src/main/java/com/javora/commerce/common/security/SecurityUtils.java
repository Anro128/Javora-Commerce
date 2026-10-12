package com.javora.commerce.common.security;

import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

	private SecurityUtils() {
	}

	public static Long userId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
