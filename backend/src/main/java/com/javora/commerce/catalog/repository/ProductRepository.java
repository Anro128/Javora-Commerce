package com.javora.commerce.catalog.repository;

import com.javora.commerce.catalog.entity.Product;
import com.javora.commerce.catalog.entity.ProductStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	boolean existsBySlug(String slug);

	boolean existsByCategoryId(Long categoryId);

	Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);

}
