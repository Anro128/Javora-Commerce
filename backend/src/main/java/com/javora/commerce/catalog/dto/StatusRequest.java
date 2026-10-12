package com.javora.commerce.catalog.dto;

import com.javora.commerce.catalog.entity.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull(message = "Status is required") ProductStatus status) {
}
