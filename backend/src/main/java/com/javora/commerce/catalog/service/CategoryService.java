package com.javora.commerce.catalog.service;

import com.javora.commerce.catalog.dto.CategoryNode;
import com.javora.commerce.catalog.dto.CategoryRequest;
import com.javora.commerce.catalog.dto.CategoryResponse;
import com.javora.commerce.catalog.entity.Category;
import com.javora.commerce.catalog.repository.CategoryRepository;
import com.javora.commerce.catalog.repository.ProductRepository;
import com.javora.commerce.common.config.CacheConfig;
import com.javora.commerce.common.exception.BusinessException;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.common.util.SlugUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private static final int SLUG_MAX_LENGTH = 120;

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	@Transactional(readOnly = true)
	public List<CategoryResponse> listAll() {
		return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream().map(CategoryResponse::from).toList();
	}

	@Cacheable(CacheConfig.CATEGORIES)
	@Transactional(readOnly = true)
	public List<CategoryNode> publicTree() {
		Map<Long, List<Category>> childrenByParent = new HashMap<>();
		List<Category> roots = new ArrayList<>();
		for (Category category : categoryRepository.findAllByOrderBySortOrderAscNameAsc()) {
			if (!category.isActive()) {
				continue;
			}
			if (category.getParentId() == null) {
				roots.add(category);
			} else {
				childrenByParent.computeIfAbsent(category.getParentId(), key -> new ArrayList<>()).add(category);
			}
		}
		return roots.stream().map(root -> toNode(root, childrenByParent)).toList();
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public CategoryResponse create(CategoryRequest request) {
		requireParentIfPresent(request.parentId());

		Category category = new Category();
		category.setParentId(request.parentId());
		category.setName(request.name().trim());
		category.setSlug(SlugUtil.uniqueSlug(request.name(), SLUG_MAX_LENGTH, categoryRepository::existsBySlug));
		apply(category, request);
		return CategoryResponse.from(categoryRepository.save(category));
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = find(id);
		requireParentIfPresent(request.parentId());
		rejectCycle(id, request.parentId());

		category.setParentId(request.parentId());
		category.setName(request.name().trim());
		apply(category, request);
		return CategoryResponse.from(category);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public void delete(Long id) {
		Category category = find(id);
		if (categoryRepository.existsByParentId(id)) {
			throw new BusinessException(HttpStatus.CONFLICT, "Delete or move the subcategories first");
		}
		if (productRepository.existsByCategoryId(id)) {
			throw new BusinessException(HttpStatus.CONFLICT, "This category still contains products");
		}
		categoryRepository.delete(category);
	}

	private void apply(Category category, CategoryRequest request) {
		category.setDescription(request.description() == null || request.description().isBlank()
				? null
				: request.description().trim());
		category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
		category.setActive(request.active() == null || request.active());
	}

	private Category find(Long id) {
		return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
	}

	private void requireParentIfPresent(Long parentId) {
		if (parentId != null && !categoryRepository.existsById(parentId)) {
			throw new ResourceNotFoundException("Parent category", parentId);
		}
	}

	// Walks up from the new parent so a category can never become its own ancestor.
	private void rejectCycle(Long categoryId, Long newParentId) {
		Long cursor = newParentId;
		while (cursor != null) {
			if (cursor.equals(categoryId)) {
				throw new BusinessException("A category cannot be moved under itself or its own subcategory");
			}
			cursor = categoryRepository.findById(cursor).map(Category::getParentId).orElse(null);
		}
	}

	private static CategoryNode toNode(Category category, Map<Long, List<Category>> childrenByParent) {
		List<CategoryNode> children = childrenByParent.getOrDefault(category.getId(), List.of()).stream()
				.map(child -> toNode(child, childrenByParent))
				.toList();
		return new CategoryNode(category.getId(), category.getName(), category.getSlug(), children);
	}

}
