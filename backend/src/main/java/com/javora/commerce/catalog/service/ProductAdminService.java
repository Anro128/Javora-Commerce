package com.javora.commerce.catalog.service;

import com.javora.commerce.catalog.dto.AdminProductResponse;
import com.javora.commerce.catalog.dto.AdminProductResponse.AdminImageResponse;
import com.javora.commerce.catalog.dto.AdminProductResponse.AdminVariantResponse;
import com.javora.commerce.catalog.dto.AdminProductSummary;
import com.javora.commerce.catalog.dto.ProductCreateRequest;
import com.javora.commerce.catalog.dto.ProductUpdateRequest;
import com.javora.commerce.catalog.dto.VariantCreateRequest;
import com.javora.commerce.catalog.dto.VariantUpdateRequest;
import com.javora.commerce.catalog.entity.Category;
import com.javora.commerce.catalog.entity.Product;
import com.javora.commerce.catalog.entity.ProductImage;
import com.javora.commerce.catalog.entity.ProductStatus;
import com.javora.commerce.catalog.entity.ProductVariant;
import com.javora.commerce.catalog.repository.CategoryRepository;
import com.javora.commerce.catalog.repository.ProductImageRepository;
import com.javora.commerce.catalog.repository.ProductRepository;
import com.javora.commerce.catalog.repository.ProductVariantRepository;
import com.javora.commerce.common.config.CacheConfig;
import com.javora.commerce.common.exception.BusinessException;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.common.storage.StorageService;
import com.javora.commerce.common.util.ImageSniffer;
import com.javora.commerce.common.util.ImageSniffer.ImageType;
import com.javora.commerce.common.util.LikePattern;
import com.javora.commerce.common.util.SlugUtil;
import com.javora.commerce.inventory.Inventory;
import com.javora.commerce.inventory.InventoryService;
import com.javora.commerce.inventory.ProductStockTotals;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductAdminService {

	static final int MAX_IMAGES_PER_PRODUCT = 8;
	static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
	private static final int SLUG_MAX_LENGTH = 220;
	private static final String IMAGE_DIRECTORY = "products";

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductVariantRepository variantRepository;
	private final ProductImageRepository imageRepository;
	private final InventoryService inventoryService;
	private final StorageService storageService;

	@Transactional(readOnly = true)
	public Page<AdminProductSummary> list(String query, ProductStatus status, Pageable pageable) {
		Specification<Product> spec = (root, criteria, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (query != null && !query.isBlank()) {
				String pattern = LikePattern.contains(query);
				predicates.add(cb.or(
						cb.like(cb.lower(root.get("name")), pattern, LikePattern.ESCAPE),
						cb.like(cb.lower(root.get("slug")), pattern, LikePattern.ESCAPE)));
			}
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};

		Page<Product> page = productRepository.findAll(spec, pageable);
		List<Long> productIds = page.getContent().stream().map(Product::getId).toList();
		List<Long> categoryIds = page.getContent().stream().map(Product::getCategoryId).distinct().toList();

		Map<Long, String> categoryNames = new HashMap<>();
		categoryRepository.findAllById(categoryIds).forEach(category -> categoryNames.put(category.getId(), category.getName()));
		Map<Long, ProductStockTotals> totals = inventoryService.totalsByProduct(productIds);
		Map<Long, String> primaryImages = new HashMap<>();
		if (!productIds.isEmpty()) {
			for (ProductImage image : imageRepository.findByProductIdInOrderBySortOrderAscIdAsc(productIds)) {
				primaryImages.putIfAbsent(image.getProductId(), storageService.publicUrl(image.getObjectKey()));
			}
		}

		List<AdminProductSummary> content = page.getContent().stream().map(product -> {
			ProductStockTotals stock = totals.get(product.getId());
			return new AdminProductSummary(
					product.getId(),
					product.getName(),
					product.getSlug(),
					categoryNames.get(product.getCategoryId()),
					product.getBasePrice(),
					product.getStatus(),
					stock == null ? 0 : stock.variantCount(),
					stock == null ? 0 : stock.available(),
					primaryImages.get(product.getId()),
					product.getCreatedAt());
		}).toList();
		return new PageImpl<>(content, pageable, page.getTotalElements());
	}

	@Transactional(readOnly = true)
	public AdminProductResponse get(Long id) {
		return toResponse(find(id));
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public AdminProductResponse create(ProductCreateRequest request) {
		requireCategory(request.categoryId());
		requireDistinctSkus(request.variants());

		Product product = new Product();
		product.setCategoryId(request.categoryId());
		product.setName(request.name().trim());
		product.setSlug(SlugUtil.uniqueSlug(request.name(), SLUG_MAX_LENGTH, productRepository::existsBySlug));
		product.setDescription(blankToNull(request.description()));
		product.setBasePrice(request.basePrice());
		product.setWeightGram(request.weightGram());
		product.setStatus(request.status() == null ? ProductStatus.DRAFT : request.status());
		productRepository.save(product);

		request.variants().forEach(variant -> createVariant(product.getId(), variant));
		return toResponse(product);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public AdminProductResponse update(Long id, ProductUpdateRequest request) {
		Product product = find(id);
		requireCategory(request.categoryId());

		product.setCategoryId(request.categoryId());
		product.setName(request.name().trim());
		product.setDescription(blankToNull(request.description()));
		product.setBasePrice(request.basePrice());
		product.setWeightGram(request.weightGram());
		product.setStatus(request.status());
		return toResponse(product);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public AdminProductResponse changeStatus(Long id, ProductStatus status) {
		Product product = find(id);
		product.setStatus(status);
		return toResponse(product);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public AdminProductResponse addVariant(Long productId, VariantCreateRequest request) {
		Product product = find(productId);
		requireSkuAvailable(request.sku().trim(), null);
		createVariant(productId, request);
		return toResponse(product);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public AdminProductResponse updateVariant(Long productId, Long variantId, VariantUpdateRequest request) {
		Product product = find(productId);
		ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
				.orElseThrow(() -> new ResourceNotFoundException("Variant", variantId));
		requireSkuAvailable(request.sku().trim(), variantId);

		variant.setSku(request.sku().trim());
		variant.setName(request.name().trim());
		variant.setPrice(request.price());
		variant.setActive(request.active());
		return toResponse(product);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public AdminProductResponse addImage(Long productId, MultipartFile file) {
		Product product = find(productId);
		if (imageRepository.countByProductId(productId) >= MAX_IMAGES_PER_PRODUCT) {
			throw new BusinessException("A product can have at most %d images".formatted(MAX_IMAGES_PER_PRODUCT));
		}
		if (file == null || file.isEmpty()) {
			throw new BusinessException("Please choose an image to upload");
		}
		if (file.getSize() > MAX_IMAGE_BYTES) {
			throw new BusinessException("Image must be at most 5 MB");
		}

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException ex) {
			throw new BusinessException("The uploaded file could not be read");
		}
		ImageType type = ImageSniffer.detect(bytes)
				.orElseThrow(() -> new BusinessException("Only JPEG, PNG, and WebP images are supported"));

		String objectKey = storageService.store(
				IMAGE_DIRECTORY, new ByteArrayInputStream(bytes), bytes.length, type.contentType(), type.extension());

		ProductImage image = new ProductImage();
		image.setProductId(productId);
		image.setObjectKey(objectKey);
		image.setSortOrder((int) imageRepository.countByProductId(productId));
		imageRepository.save(image);
		return toResponse(product);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public AdminProductResponse makePrimaryImage(Long productId, Long imageId) {
		Product product = find(productId);
		List<ProductImage> images = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId);
		ProductImage target = images.stream().filter(image -> image.getId().equals(imageId)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Image", imageId));

		int order = 0;
		target.setSortOrder(order++);
		for (ProductImage image : images) {
			if (!image.getId().equals(imageId)) {
				image.setSortOrder(order++);
			}
		}
		return toResponse(product);
	}

	@Transactional
	@CacheEvict(cacheNames = {CacheConfig.CATEGORIES, CacheConfig.PRODUCT_CONTENT}, allEntries = true)
	public AdminProductResponse deleteImage(Long productId, Long imageId) {
		Product product = find(productId);
		ProductImage image = imageRepository.findByIdAndProductId(imageId, productId)
				.orElseThrow(() -> new ResourceNotFoundException("Image", imageId));
		imageRepository.delete(image);
		storageService.delete(image.getObjectKey());
		return toResponse(product);
	}

	private void createVariant(Long productId, VariantCreateRequest request) {
		requireSkuAvailable(request.sku().trim(), null);
		ProductVariant variant = new ProductVariant();
		variant.setProductId(productId);
		variant.setSku(request.sku().trim());
		variant.setName(request.name().trim());
		variant.setPrice(request.price());
		variant.setActive(true);
		variantRepository.save(variant);
		inventoryService.initialize(variant.getId(), request.stock());
	}

	private void requireDistinctSkus(List<VariantCreateRequest> variants) {
		Set<String> seen = new HashSet<>();
		for (VariantCreateRequest variant : variants) {
			if (!seen.add(variant.sku().trim().toLowerCase())) {
				throw new BusinessException(HttpStatus.CONFLICT, "SKU appears more than once: " + variant.sku());
			}
		}
	}

	private void requireSkuAvailable(String sku, Long ownVariantId) {
		boolean taken = ownVariantId == null
				? variantRepository.existsBySku(sku)
				: variantRepository.existsBySkuAndIdNot(sku, ownVariantId);
		if (taken) {
			throw new BusinessException(HttpStatus.CONFLICT, "SKU is already in use: " + sku);
		}
	}

	private void requireCategory(Long categoryId) {
		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
		if (!category.isActive()) {
			throw new BusinessException("The selected category is not active");
		}
	}

	private Product find(Long id) {
		return productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product", id));
	}

	private AdminProductResponse toResponse(Product product) {
		List<AdminImageResponse> images = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).stream()
				.map(image -> new AdminImageResponse(
						image.getId(), storageService.publicUrl(image.getObjectKey()), image.getSortOrder()))
				.toList();

		List<ProductVariant> variants = variantRepository.findByProductIdOrderByIdAsc(product.getId());
		Map<Long, Inventory> inventory = inventoryService.byVariant(variants.stream().map(ProductVariant::getId).toList());
		List<AdminVariantResponse> variantResponses = variants.stream().map(variant -> {
			Inventory stock = inventory.get(variant.getId());
			return new AdminVariantResponse(
					variant.getId(),
					variant.getSku(),
					variant.getName(),
					variant.getPrice(),
					variant.isActive(),
					stock == null ? 0 : stock.getQuantity(),
					stock == null ? 0 : stock.getReservedQuantity());
		}).toList();

		return new AdminProductResponse(
				product.getId(),
				product.getCategoryId(),
				product.getName(),
				product.getSlug(),
				product.getDescription(),
				product.getBasePrice(),
				product.getWeightGram(),
				product.getStatus(),
				images,
				variantResponses,
				product.getCreatedAt(),
				product.getUpdatedAt());
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
