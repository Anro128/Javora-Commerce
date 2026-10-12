package com.javora.commerce.catalog.service;

import com.javora.commerce.catalog.dto.ProductContent;
import com.javora.commerce.catalog.dto.ProductContent.ImageView;
import com.javora.commerce.catalog.dto.ProductContent.VariantContent;
import com.javora.commerce.catalog.entity.Category;
import com.javora.commerce.catalog.entity.Product;
import com.javora.commerce.catalog.entity.ProductStatus;
import com.javora.commerce.catalog.repository.CategoryRepository;
import com.javora.commerce.catalog.repository.ProductImageRepository;
import com.javora.commerce.catalog.repository.ProductRepository;
import com.javora.commerce.catalog.repository.ProductVariantRepository;
import com.javora.commerce.common.config.CacheConfig;
import com.javora.commerce.common.exception.ResourceNotFoundException;
import com.javora.commerce.common.storage.StorageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Lives in its own bean so the cache proxy is applied to calls from ProductQueryService.
@Service
@RequiredArgsConstructor
public class ProductContentLoader {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductVariantRepository variantRepository;
	private final ProductImageRepository imageRepository;
	private final StorageService storageService;

	@Cacheable(cacheNames = CacheConfig.PRODUCT_CONTENT, key = "#slug")
	@Transactional(readOnly = true)
	public ProductContent loadActive(String slug) {
		Product product = productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
				.orElseThrow(() -> new ResourceNotFoundException("Product", slug));
		Category category = categoryRepository.findById(product.getCategoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Category", product.getCategoryId()));

		List<ImageView> images = imageRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId()).stream()
				.map(image -> new ImageView(image.getId(), storageService.publicUrl(image.getObjectKey())))
				.toList();
		List<VariantContent> variants = variantRepository.findByProductIdOrderByIdAsc(product.getId()).stream()
				.filter(variant -> variant.isActive())
				.map(variant -> new VariantContent(variant.getId(), variant.getSku(), variant.getName(), variant.getPrice()))
				.toList();

		return new ProductContent(
				product.getId(),
				product.getName(),
				product.getSlug(),
				product.getDescription(),
				product.getBasePrice(),
				product.getWeightGram(),
				category.getName(),
				category.getSlug(),
				images,
				variants);
	}

}
