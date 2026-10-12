package com.javora.commerce.order;

import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.response.PageResponse;
import com.javora.commerce.common.security.SecurityUtils;
import com.javora.commerce.order.dto.CheckoutPreviewResponse;
import com.javora.commerce.order.dto.CheckoutRequest;
import com.javora.commerce.order.dto.OrderResponse;
import com.javora.commerce.order.dto.OrderSummaryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

	private static final int MAX_PAGE_SIZE = 50;

	private final CheckoutService checkoutService;
	private final OrderService orderService;

	@PostMapping("/checkout/preview")
	public ApiResponse<CheckoutPreviewResponse> preview(
			@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
		return ApiResponse.ok(checkoutService.preview(SecurityUtils.userId(jwt), request));
	}

	@PostMapping("/checkout")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<OrderResponse> checkout(
			@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
		return ApiResponse.ok("Order placed", checkoutService.placeOrder(SecurityUtils.userId(jwt), request));
	}

	@GetMapping("/orders")
	public ApiResponse<PageResponse<OrderSummaryResponse>> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE));
		return ApiResponse.ok(PageResponse.from(orderService.list(SecurityUtils.userId(jwt), pageable)));
	}

	@GetMapping("/orders/{orderNumber}")
	public ApiResponse<OrderResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber) {
		return ApiResponse.ok(orderService.get(SecurityUtils.userId(jwt), orderNumber));
	}

	@PostMapping("/orders/{orderNumber}/cancel")
	public ApiResponse<OrderResponse> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber) {
		return ApiResponse.ok("Order cancelled", orderService.cancel(SecurityUtils.userId(jwt), orderNumber));
	}

}
