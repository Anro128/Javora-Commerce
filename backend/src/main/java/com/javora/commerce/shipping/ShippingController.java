package com.javora.commerce.shipping;

import com.javora.commerce.cart.CartService;
import com.javora.commerce.cart.dto.CartResponse;
import com.javora.commerce.common.exception.BusinessException;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.security.SecurityUtils;
import com.javora.commerce.user.entity.Address;
import com.javora.commerce.user.repository.AddressRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {

	private final ShippingRateProvider rateProvider;
	private final CartService cartService;
	private final AddressRepository addressRepository;

	@GetMapping("/options")
	public ApiResponse<List<ShippingOption>> options(@AuthenticationPrincipal Jwt jwt, @RequestParam Long addressId) {
		Long userId = SecurityUtils.userId(jwt);
		Address address = addressRepository.findByIdAndUserId(addressId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

		CartResponse cart = CartResponse.of(cartService.lines(userId));
		if (cart.items().isEmpty()) {
			throw new BusinessException("Your cart is empty");
		}
		return ApiResponse.ok(rateProvider.quote(address, cart.totalWeightGram()));
	}

}
