package com.javora.commerce.catalog.repository;

import com.javora.commerce.catalog.entity.ProductImage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

	List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);

	List<ProductImage> findByProductIdInOrderBySortOrderAscIdAsc(Collection<Long> productIds);

	Optional<ProductImage> findByIdAndProductId(Long id, Long productId);

	long countByProductId(Long productId);

}
