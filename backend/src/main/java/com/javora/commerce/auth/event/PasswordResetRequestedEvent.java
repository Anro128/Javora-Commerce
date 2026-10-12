package com.javora.commerce.auth.event;

public record PasswordResetRequestedEvent(String email, String fullName, String resetToken) {
}
