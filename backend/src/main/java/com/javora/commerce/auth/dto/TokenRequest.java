package com.javora.commerce.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@NotBlank(message = "Token is required") String token) {
}
