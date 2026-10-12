package com.javora.commerce.catalog.repository;

import com.javora.commerce.catalog.entity.Category;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsBySlug(String slug);

	boolean existsByParentId(Long parentId);

	List<Category> findAllByOrderBySortOrderAscNameAsc();

}
