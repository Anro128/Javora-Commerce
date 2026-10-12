package com.javora.commerce.order;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(Duration paymentWindow) {

	public OrderProperties {
		paymentWindow = paymentWindow == null ? Duration.ofHours(24) : paymentWindow;
	}

	@Configuration
	@EnableConfigurationProperties(OrderProperties.class)
	static class Registration {
	}

}
