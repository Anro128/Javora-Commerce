package com.javora.commerce.catalog.controller;

import com.javora.commerce.catalog.dto.CategoryNode;
import com.javora.commerce.catalog.dto.ProductDetailResponse;
import com.javora.commerce.catalog.dto.ProductSummary;
import com.javora.commerce.catalog.service.CategoryService;
import com.javora.commerce.catalog.service.ProductQueryService;
import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.response.PageResponse;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicCatalogController {

	private final CategoryService categoryService;
	private final ProductQueryService productQueryService;

	@GetMapping("/categories")
	public ApiResponse<List<CategoryNode>> categories() {
		return ApiResponse.ok(categoryService.publicTree());
	}

	@GetMapping("/products")
	public ApiResponse<PageResponse<ProductSummary>> products(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String category,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(required = false) String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "12") int size) {
		return ApiResponse.ok(productQueryService.search(q, category, minPrice, maxPrice, sort, page, size));
	}

	@GetMapping("/products/{slug}")
	public ApiResponse<ProductDetailResponse> product(@PathVariable String slug) {
		return ApiResponse.ok(productQueryService.getBySlug(slug));
	}

}
