package com.javora.commerce.auth.event;

public record VerificationRequestedEvent(String email, String fullName, String verificationToken) {
}
