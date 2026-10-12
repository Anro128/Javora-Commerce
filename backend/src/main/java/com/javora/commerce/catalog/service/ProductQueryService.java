package com.javora.commerce.catalog.service;

import com.javora.commerce.catalog.dto.CategoryNode;
import com.javora.commerce.catalog.dto.ProductContent;
import com.javora.commerce.catalog.dto.ProductDetailResponse;
import com.javora.commerce.catalog.dto.ProductDetailResponse.VariantResponse;
import com.javora.commerce.catalog.dto.ProductSummary;
import com.javora.commerce.catalog.entity.Category;
import com.javora.commerce.catalog.entity.Product;
import com.javora.commerce.catalog.entity.ProductImage;
import com.javora.commerce.catalog.entity.ProductStatus;
import com.javora.commerce.catalog.repository.CategoryRepository;
import com.javora.commerce.catalog.repository.ProductImageRepository;
import com.javora.commerce.catalog.repository.ProductRepository;
import com.javora.commerce.common.response.PageResponse;
import com.javora.commerce.common.storage.StorageService;
import com.javora.commerce.common.util.LikePattern;
import com.javora.commerce.inventory.InventoryService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProductQueryService {

	static final int MAX_PAGE_SIZE = 48;

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductImageRepository imageRepository;
	private final CategoryService categoryService;
	private final ProductContentLoader contentLoader;
	private final InventoryService inventoryService;
	private final StorageService storageService;

	@Transactional(readOnly = true)
	public PageResponse<ProductSummary> search(
			String query, String categorySlug, BigDecimal minPrice, BigDecimal maxPrice, String sort, int page, int size) {
		Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE), sortFor(sort));

		Set<Long> categoryIds = null;
		if (StringUtils.hasText(categorySlug)) {
			categoryIds = categoryIdsFor(categorySlug);
			if (categoryIds.isEmpty()) {
				return PageResponse.from(Page.empty(pageable));
			}
		}

		Page<Product> products = productRepository.findAll(
				specification(query, categoryIds, minPrice, maxPrice), pageable);
		return PageResponse.from(new PageImpl<>(toSummaries(products.getContent()), pageable, products.getTotalElements()));
	}

	@Transactional(readOnly = true)
	public ProductDetailResponse getBySlug(String slug) {
		ProductContent content = contentLoader.loadActive(slug);
		Map<Long, Integer> availability = inventoryService.availability(
				content.variants().stream().map(ProductContent.VariantContent::id).toList());

		List<VariantResponse> variants = content.variants().stream()
				.map(variant -> {
					int available = availability.getOrDefault(variant.id(), 0);
					return new VariantResponse(variant.id(), variant.sku(), variant.name(), variant.price(), available, available > 0);
				})
				.toList();
		return new ProductDetailResponse(
				content.id(),
				content.name(),
				content.slug(),
				content.description(),
				content.price(),
				content.weightGram(),
				content.categoryName(),
				content.categorySlug(),
				content.images(),
				variants);
	}

	private Specification<Product> specification(
			String query, Set<Long> categoryIds, BigDecimal minPrice, BigDecimal maxPrice) {
		return (root, criteria, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("status"), ProductStatus.ACTIVE));
			if (categoryIds != null) {
				predicates.add(root.get("categoryId").in(categoryIds));
			}
			if (StringUtils.hasText(query)) {
				String pattern = LikePattern.contains(query);
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("name")), pattern, LikePattern.ESCAPE),
						cb.like(cb.lower(root.get("description")), pattern, LikePattern.ESCAPE)));
			}
			if (minPrice != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice));
			}
			if (maxPrice != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
	}

	private static Sort sortFor(String sort) {
		Sort tieBreaker = Sort.by(Sort.Direction.DESC, "id");
		return switch (sort == null ? "" : sort) {
			case "price_asc" -> Sort.by(Sort.Direction.ASC, "basePrice").and(tieBreaker);
			case "price_desc" -> Sort.by(Sort.Direction.DESC, "basePrice").and(tieBreaker);
			case "name" -> Sort.by(Sort.Direction.ASC, "name").and(tieBreaker);
			default -> Sort.by(Sort.Direction.DESC, "createdAt").and(tieBreaker);
		};
	}

	// A category filter includes every subcategory below it.
	private Set<Long> categoryIdsFor(String slug) {
		Set<Long> ids = new HashSet<>();
		findNode(categoryService.publicTree(), slug).ifPresent(node -> collect(node, ids));
		return ids;
	}

	private static Optional<CategoryNode> findNode(List<CategoryNode> nodes, String slug) {
		for (CategoryNode node : nodes) {
			if (node.slug().equals(slug)) {
				return Optional.of(node);
			}
			Optional<CategoryNode> inChild = findNode(node.children(), slug);
			if (inChild.isPresent()) {
				return inChild;
			}
		}
		return Optional.empty();
	}

	private static void collect(CategoryNode node, Set<Long> ids) {
		ids.add(node.id());
		node.children().forEach(child -> collect(child, ids));
	}

	private List<ProductSummary> toSummaries(List<Product> products) {
		if (products.isEmpty()) {
			return List.of();
		}
		List<Long> productIds = products.stream().map(Product::getId).toList();
		List<Long> categoryIds = products.stream().map(Product::getCategoryId).distinct().toList();

		Map<Long, Category> categories = new HashMap<>();
		categoryRepository.findAllById(categoryIds).forEach(category -> categories.put(category.getId(), category));

		Map<Long, String> primaryImages = new HashMap<>();
		for (ProductImage image : imageRepository.findByProductIdInOrderBySortOrderAscIdAsc(productIds)) {
			primaryImages.putIfAbsent(image.getProductId(), storageService.publicUrl(image.getObjectKey()));
		}
		Set<Long> inStock = new HashSet<>(inventoryService.productIdsInStock(productIds));

		return products.stream().map(product -> {
			Category category = categories.get(product.getCategoryId());
			return new ProductSummary(
					product.getId(),
					product.getName(),
					product.getSlug(),
					product.getBasePrice(),
					primaryImages.get(product.getId()),
					category == null ? null : category.getName(),
					category == null ? null : category.getSlug(),
					inStock.contains(product.getId()));
		}).toList();
	}

}
