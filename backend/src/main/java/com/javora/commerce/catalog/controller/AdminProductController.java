package com.javora.commerce.catalog.controller;

import com.javora.commerce.catalog.dto.AdminProductResponse;
import com.javora.commerce.catalog.dto.AdminProductSummary;
import com.javora.commerce.catalog.dto.ProductCreateRequest;
import com.javora.commerce.catalog.dto.ProductUpdateRequest;
import com.javora.commerce.catalog.dto.StatusRequest;
import com.javora.commerce.catalog.dto.VariantCreateRequest;
import com.javora.commerce.catalog.dto.VariantUpdateRequest;
import com.javora.commerce.catalog.entity.ProductStatus;
import com.javora.commerce.catalog.service.ProductAdminService;
import com.javora.commerce.common.response.ApiResponse;
import com.javora.commerce.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

	private static final int MAX_PAGE_SIZE = 100;

	private final ProductAdminService productAdminService;

	@GetMapping
	public ApiResponse<PageResponse<AdminProductSummary>> list(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) ProductStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		PageRequest pageable = PageRequest.of(
				Math.max(0, page),
				Math.min(Math.max(1, size), MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
		return ApiResponse.ok(PageResponse.from(productAdminService.list(q, status, pageable)));
	}

	@GetMapping("/{id}")
	public ApiResponse<AdminProductResponse> get(@PathVariable Long id) {
		return ApiResponse.ok(productAdminService.get(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
		return ApiResponse.ok("Product created", productAdminService.create(request));
	}

	@PutMapping("/{id}")
	public ApiResponse<AdminProductResponse> update(
			@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
		return ApiResponse.ok("Product updated", productAdminService.update(id, request));
	}

	@PatchMapping("/{id}/status")
	public ApiResponse<AdminProductResponse> changeStatus(
			@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
		return ApiResponse.ok("Status updated", productAdminService.changeStatus(id, request.status()));
	}

	// Products are archived instead of deleted so past orders keep a valid reference.
	@DeleteMapping("/{id}")
	public ApiResponse<AdminProductResponse> archive(@PathVariable Long id) {
		return ApiResponse.ok("Product archived", productAdminService.changeStatus(id, ProductStatus.ARCHIVED));
	}

	@PostMapping("/{id}/variants")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminProductResponse> addVariant(
			@PathVariable Long id, @Valid @RequestBody VariantCreateRequest request) {
		return ApiResponse.ok("Variant added", productAdminService.addVariant(id, request));
	}

	@PutMapping("/{id}/variants/{variantId}")
	public ApiResponse<AdminProductResponse> updateVariant(
			@PathVariable Long id, @PathVariable Long variantId, @Valid @RequestBody VariantUpdateRequest request) {
		return ApiResponse.ok("Variant updated", productAdminService.updateVariant(id, variantId, request));
	}

	@PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<AdminProductResponse> addImage(
			@PathVariable Long id, @RequestPart("file") MultipartFile file) {
		return ApiResponse.ok("Image uploaded", productAdminService.addImage(id, file));
	}

	@PutMapping("/{id}/images/{imageId}/primary")
	public ApiResponse<AdminProductResponse> makePrimary(@PathVariable Long id, @PathVariable Long imageId) {
		return ApiResponse.ok("Primary image updated", productAdminService.makePrimaryImage(id, imageId));
	}

	@DeleteMapping("/{id}/images/{imageId}")
	public ApiResponse<AdminProductResponse> deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
		return ApiResponse.ok("Image deleted", productAdminService.deleteImage(id, imageId));
	}

}
