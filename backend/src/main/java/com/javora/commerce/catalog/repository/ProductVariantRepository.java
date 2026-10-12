package com.javora.commerce.catalog.repository;

import com.javora.commerce.catalog.entity.ProductVariant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

	List<ProductVariant> findByProductIdOrderByIdAsc(Long productId);

	List<ProductVariant> findByProductIdInOrderByIdAsc(Collection<Long> productIds);

	Optional<ProductVariant> findByIdAndProductId(Long id, Long productId);

	boolean existsBySku(String sku);

	boolean existsBySkuAndIdNot(String sku, Long id);

}
