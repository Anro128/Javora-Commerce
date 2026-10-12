package com.javora.commerce.order;

import com.javora.commerce.cart.CartService;
import com.javora.commerce.cart.dto.CartLine;
import com.javora.commerce.cart.dto.CartResponse;
import com.javora.commerce.common.exception.BusinessException;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.inventory.InsufficientStockException;
import com.javora.commerce.inventory.InventoryService;
import com.javora.commerce.order.dto.CheckoutPreviewResponse;
import com.javora.commerce.order.dto.CheckoutRequest;
import com.javora.commerce.order.dto.OrderResponse;
import com.javora.commerce.order.dto.ShippingAddress;
import com.javora.commerce.order.repository.OrderItemRepository;
import com.javora.commerce.order.repository.OrderRepository;
import com.javora.commerce.shipping.ShippingOption;
import com.javora.commerce.shipping.ShippingRateProvider;
import com.javora.commerce.user.entity.Address;
import com.javora.commerce.user.repository.AddressRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckoutService {

	private final CartService cartService;
	private final AddressRepository addressRepository;
	private final ShippingRateProvider shippingRateProvider;
	private final InventoryService inventoryService;
	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final OrderProperties orderProperties;

	private record Prepared(List<CartLine> lines, CartResponse cart, Address address, ShippingOption shipping) {

		BigDecimal total() {
			return cart.subtotal().add(shipping.cost());
		}

	}

	@Transactional
	public CheckoutPreviewResponse preview(Long userId, CheckoutRequest request) {
		Prepared prepared = prepare(userId, request);
		return new CheckoutPreviewResponse(
				prepared.lines(),
				prepared.cart().subtotal(),
				prepared.shipping().cost(),
				BigDecimal.ZERO,
				prepared.total(),
				prepared.cart().totalWeightGram(),
				prepared.shipping(),
				ShippingAddress.from(prepared.address()));
	}

	// Reservation, order creation, and clearing the cart commit together or not at all.
	@Transactional
	public OrderResponse placeOrder(Long userId, CheckoutRequest request) {
		Prepared prepared = prepare(userId, request);

		// Reserving in a stable order keeps concurrent checkouts from deadlocking each other.
		List<CartLine> ordered = prepared.lines().stream().sorted(Comparator.comparing(CartLine::variantId)).toList();
		for (CartLine line : ordered) {
			try {
				inventoryService.reserve(line.variantId(), line.quantity());
			} catch (InsufficientStockException ex) {
				throw new BusinessException(HttpStatus.CONFLICT,
						"Not enough stock for %s (%s)".formatted(line.productName(), line.variantName()));
			}
		}

		Order order = new Order();
		order.setOrderNumber(uniqueOrderNumber());
		order.setUserId(userId);
		order.setSubtotal(prepared.cart().subtotal());
		order.setShippingCost(prepared.shipping().cost());
		order.setDiscount(BigDecimal.ZERO);
		order.setTotal(prepared.total());
		order.setCourier(prepared.shipping().courier());
		order.setCourierService(prepared.shipping().service());
		order.setRecipientName(prepared.address().getRecipientName());
		order.setPhone(prepared.address().getPhone());
		order.setProvince(prepared.address().getProvince());
		order.setCity(prepared.address().getCity());
		order.setDistrict(prepared.address().getDistrict());
		order.setPostalCode(prepared.address().getPostalCode());
		order.setStreet(prepared.address().getStreet());
		order.setExpiresAt(Instant.now().plus(orderProperties.paymentWindow()));
		orderRepository.save(order);

		List<OrderItem> items = prepared.lines().stream().map(line -> toItem(order, line)).toList();
		orderItemRepository.saveAll(items);

		cartService.clear(userId);
		return OrderResponse.from(order, items);
	}

	private Prepared prepare(Long userId, CheckoutRequest request) {
		List<CartLine> lines = cartService.lines(userId);
		if (lines.isEmpty()) {
			throw new BusinessException("Your cart is empty");
		}
		for (CartLine line : lines) {
			if (CartLine.UNAVAILABLE.equals(line.issue())) {
				throw new BusinessException(HttpStatus.CONFLICT,
						"%s (%s) is no longer available".formatted(line.productName(), line.variantName()));
			}
			if (line.hasIssue()) {
				throw new BusinessException(HttpStatus.CONFLICT, line.available() == 0
						? "%s (%s) is out of stock".formatted(line.productName(), line.variantName())
						: "Only %d left of %s (%s)".formatted(line.available(), line.productName(), line.variantName()));
			}
		}

		Address address = addressRepository.findByIdAndUserId(request.addressId(), userId)
				.orElseThrow(() -> new ResourceNotFoundException("Address", request.addressId()));
		CartResponse cart = CartResponse.of(lines);

		// The price always comes from the provider, never from the client.
		ShippingOption shipping = shippingRateProvider.quote(address, cart.totalWeightGram()).stream()
				.filter(option -> option.courier().equals(request.courier()) && option.service().equals(request.service()))
				.findFirst()
				.orElseThrow(() -> new BusinessException("The selected shipping service is not available"));
		return new Prepared(lines, cart, address, shipping);
	}

	private OrderItem toItem(Order order, CartLine line) {
		OrderItem item = new OrderItem();
		item.setOrderId(order.getId());
		item.setVariantId(line.variantId());
		item.setProductId(line.productId());
		item.setProductName(line.productName());
		item.setVariantName(line.variantName());
		item.setSku(line.sku());
		item.setUnitPrice(line.unitPrice());
		item.setQuantity(line.quantity());
		item.setWeightGram(line.weightGram());
		item.setLineTotal(line.lineTotal());
		return item;
	}

	private String uniqueOrderNumber() {
		String candidate = OrderNumbers.next();
		while (orderRepository.existsByOrderNumber(candidate)) {
			candidate = OrderNumbers.next();
		}
		return candidate;
	}

}
