package com.javora.commerce.auth.service;

import com.javora.commerce.auth.dto.AuthResponse;

public record Session(AuthResponse response, String refreshToken) {
}
