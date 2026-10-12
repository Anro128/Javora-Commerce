package com.javora.commerce.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
		String jwtSecret,
		Duration accessTokenTtl,
		Duration refreshTokenTtl,
		Duration verificationTokenTtl,
		Duration passwordResetTokenTtl,
		boolean cookieSecure) {

	private static final int MIN_SECRET_BYTES = 32;

	public AuthProperties {
		if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException(
					"app.auth.jwt-secret (JWT_SECRET) must be set and at least %d bytes long".formatted(MIN_SECRET_BYTES));
		}
	}

}
