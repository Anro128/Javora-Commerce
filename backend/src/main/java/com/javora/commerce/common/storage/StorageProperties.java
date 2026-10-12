package com.javora.commerce.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
		String endpoint,
		String accessKey,
		String secretKey,
		String bucket,
		String publicBaseUrl) {
}
