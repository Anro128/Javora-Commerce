package com.javora.commerce.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

	public static final String CATEGORIES = "categories";
	public static final String PRODUCT_CONTENT = "product-content";

}
