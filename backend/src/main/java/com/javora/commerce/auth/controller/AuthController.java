package com.javora.commerce.auth.controller;

import com.javora.commerce.auth.AuthProperties;
import com.javora.commerce.auth.dto.AuthResponse;
import com.javora.commerce.auth.dto.ChangePasswordRequest;
import com.javora.commerce.auth.dto.ForgotPasswordRequest;
import com.javora.commerce.auth.dto.LoginRequest;
import com.javora.commerce.auth.dto.RegisterRequest;
import com.javora.commerce.auth.dto.ResetPasswordRequest;
import com.javora.commerce.auth.dto.TokenRequest;
import com.javora.commerce.auth.service.AuthService;
import com.javora.commerce.auth.service.Session;
import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.security.SecurityUtils;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	static final String REFRESH_COOKIE = "refresh_token";
	static final String COOKIE_PATH = "/api/v1/auth";

	private final AuthService authService;
	private final AuthProperties properties;

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
		return withSession(HttpStatus.CREATED, "Registration successful", authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
		return withSession(HttpStatus.OK, "Login successful", authService.login(request));
	}

	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<AuthResponse>> refresh(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
		return withSession(HttpStatus.OK, "Session refreshed", authService.refresh(refreshToken));
	}

	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(
			@CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
		authService.logout(refreshToken);
		return ResponseEntity.ok()
				.header(HttpHeaders.SET_COOKIE, refreshCookie("", Duration.ZERO).toString())
				.body(ApiResponse.ok("Logged out", null));
	}

	@PostMapping("/verify-email")
	public ApiResponse<Void> verifyEmail(@Valid @RequestBody TokenRequest request) {
		authService.verifyEmail(request.token());
		return ApiResponse.ok("Email verified", null);
	}

	@PostMapping("/resend-verification")
	public ApiResponse<Void> resendVerification(@AuthenticationPrincipal Jwt jwt) {
		authService.resendVerification(SecurityUtils.userId(jwt));
		return ApiResponse.ok("Verification email sent", null);
	}

	@PostMapping("/forgot-password")
	public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		authService.forgotPassword(request.email());
		return ApiResponse.ok("If the email is registered, a reset link has been sent", null);
	}

	@PostMapping("/reset-password")
	public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		authService.resetPassword(request.token(), request.newPassword());
		return ApiResponse.ok("Password has been reset", null);
	}

	@PostMapping("/change-password")
	public ApiResponse<Void> changePassword(
			@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(SecurityUtils.userId(jwt), request);
		return ApiResponse.ok("Password changed", null);
	}

	private ResponseEntity<ApiResponse<AuthResponse>> withSession(HttpStatus status, String message, Session session) {
		ResponseCookie cookie = refreshCookie(session.refreshToken(), properties.refreshTokenTtl());
		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, cookie.toString())
				.body(ApiResponse.ok(message, session.response()));
	}

	private ResponseCookie refreshCookie(String value, Duration maxAge) {
		return ResponseCookie.from(REFRESH_COOKIE, value)
				.httpOnly(true)
				.secure(properties.cookieSecure())
				.sameSite("Strict")
				.path(COOKIE_PATH)
				.maxAge(maxAge)
				.build();
	}

}
