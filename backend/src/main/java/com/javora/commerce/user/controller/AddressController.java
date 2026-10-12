package com.javora.commerce.user.controller;

import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.security.SecurityUtils;
import com.javora.commerce.user.dto.AddressRequest;
import com.javora.commerce.user.dto.AddressResponse;
import com.javora.commerce.user.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
public class AddressController {

	private final AddressService addressService;

	@GetMapping
	public ApiResponse<List<AddressResponse>> list(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok(addressService.list(SecurityUtils.userId(jwt)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AddressResponse> create(
			@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddressRequest request) {
		return ApiResponse.ok("Address created", addressService.create(SecurityUtils.userId(jwt), request));
	}

	@PutMapping("/{id}")
	public ApiResponse<AddressResponse> update(
			@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody AddressRequest request) {
		return ApiResponse.ok("Address updated", addressService.update(SecurityUtils.userId(jwt), id, request));
	}

	@PutMapping("/{id}/default")
	public ApiResponse<AddressResponse> setDefault(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		return ApiResponse.ok("Default address updated", addressService.setDefault(SecurityUtils.userId(jwt), id));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
		addressService.delete(SecurityUtils.userId(jwt), id);
	}

}
