package com.javora.commerce.cart.repository;

import com.javora.commerce.cart.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

	List<CartItem> findByCartIdOrderByIdAsc(Long cartId);

	Optional<CartItem> findByCartIdAndVariantId(Long cartId, Long variantId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from CartItem i where i.cartId = :cartId")
	void deleteAllByCartId(@Param("cartId") Long cartId);

}
