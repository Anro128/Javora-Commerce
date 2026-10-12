package com.javora.commerce.order.repository;

import com.javora.commerce.order.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

	boolean existsByOrderNumber(String orderNumber);

	Page<Order> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

}
