package com.javora.commerce.user.controller;

import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.security.SecurityUtils;
import com.javora.commerce.user.dto.UpdateProfileRequest;
import com.javora.commerce.user.dto.UserResponse;
import com.javora.commerce.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public ApiResponse<UserResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok(userService.getProfile(SecurityUtils.userId(jwt)));
	}

	@PutMapping
	public ApiResponse<UserResponse> updateProfile(
			@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
		return ApiResponse.ok("Profile updated", userService.updateProfile(SecurityUtils.userId(jwt), request));
	}

}
