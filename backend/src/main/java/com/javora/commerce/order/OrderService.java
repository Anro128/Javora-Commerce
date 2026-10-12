package com.javora.commerce.order;

import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.inventory.InventoryService;
import com.javora.commerce.order.dto.OrderResponse;
import com.javora.commerce.order.dto.OrderSummaryResponse;
import com.javora.commerce.order.repository.OrderItemRepository;
import com.javora.commerce.order.repository.OrderRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderItemRepository orderItemRepository;
	private final InventoryService inventoryService;

	@Transactional(readOnly = true)
	public Page<OrderSummaryResponse> list(Long userId, Pageable pageable) {
		Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
		Map<Long, Integer> itemCounts = new HashMap<>();
		if (!orders.isEmpty()) {
			orderItemRepository.findByOrderIdIn(orders.getContent().stream().map(Order::getId).toList())
					.forEach(item -> itemCounts.merge(item.getOrderId(), item.getQuantity(), Integer::sum));
		}
		List<OrderSummaryResponse> content = orders.getContent().stream()
				.map(order -> new OrderSummaryResponse(
						order.getOrderNumber(), order.getStatus(), order.getTotal(),
						itemCounts.getOrDefault(order.getId(), 0), order.getCreatedAt()))
				.toList();
		return new PageImpl<>(content, pageable, orders.getTotalElements());
	}

	// Another user's order number looks exactly like one that does not exist.
	@Transactional(readOnly = true)
	public OrderResponse get(Long userId, String orderNumber) {
		Order order = find(userId, orderNumber);
		return OrderResponse.from(order, orderItemRepository.findByOrderIdOrderByIdAsc(order.getId()));
	}

	@Transactional
	public OrderResponse cancel(Long userId, String orderNumber) {
		Order order = find(userId, orderNumber);
		order.transitionTo(OrderStatus.CANCELLED);
		order.setCancelledAt(Instant.now());

		List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(order.getId());
		items.forEach(item -> inventoryService.release(item.getVariantId(), item.getQuantity()));
		return OrderResponse.from(order, items);
	}

	private Order find(Long userId, String orderNumber) {
		return orderRepository.findByOrderNumberAndUserId(orderNumber, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order", orderNumber));
	}

}
