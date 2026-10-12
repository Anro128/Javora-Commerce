package com.javora.commerce.cart;

import com.javora.commerce.cart.dto.CartItemRequest;
import com.javora.commerce.cart.dto.CartItemsRequest;
import com.javora.commerce.cart.dto.CartResponse;
import com.javora.commerce.cart.dto.QuantityRequest;
import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.security.SecurityUtils;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CartController {

	private final CartService cartService;

	@GetMapping("/cart")
	public ApiResponse<CartResponse> get(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok(cartService.getCart(SecurityUtils.userId(jwt)));
	}

	@PostMapping("/cart/items")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<CartResponse> add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CartItemRequest request) {
		return ApiResponse.ok("Item added",
				cartService.addItem(SecurityUtils.userId(jwt), request.variantId(), request.quantity()));
	}

	@PutMapping("/cart/items/{variantId}")
	public ApiResponse<CartResponse> update(
			@AuthenticationPrincipal Jwt jwt, @PathVariable Long variantId, @Valid @RequestBody QuantityRequest request) {
		return ApiResponse.ok(cartService.updateItem(SecurityUtils.userId(jwt), variantId, request.quantity()));
	}

	@DeleteMapping("/cart/items/{variantId}")
	public ApiResponse<CartResponse> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long variantId) {
		return ApiResponse.ok(cartService.removeItem(SecurityUtils.userId(jwt), variantId));
	}

	@DeleteMapping("/cart")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void clear(@AuthenticationPrincipal Jwt jwt) {
		cartService.clear(SecurityUtils.userId(jwt));
	}

	@PostMapping("/cart/merge")
	public ApiResponse<CartResponse> merge(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CartItemsRequest request) {
		return ApiResponse.ok(cartService.merge(SecurityUtils.userId(jwt), request.items()));
	}

	// Lets a guest see current prices and stock for the items kept in the browser.
	@PostMapping("/public/cart/preview")
	public ApiResponse<CartResponse> preview(@Valid @RequestBody CartItemsRequest request) {
		return ApiResponse.ok(cartService.preview(request.items()));
	}

}
