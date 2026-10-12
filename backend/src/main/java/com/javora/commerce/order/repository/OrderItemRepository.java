package com.javora.commerce.order.repository;

import com.javora.commerce.order.OrderItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

	List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);

}
