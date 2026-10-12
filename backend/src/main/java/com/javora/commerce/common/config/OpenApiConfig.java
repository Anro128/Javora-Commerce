package com.javora.commerce.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI javoraOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Javora Commerce API")
				.description("REST API for the Javora Commerce e-commerce platform")
				.version("v1"));
	}

}
