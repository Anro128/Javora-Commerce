package com.javora.commerce.web;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

	private static final String[] BACKEND_PREFIXES = {"api/", "actuator/", "v3/api-docs", "swagger-ui"};

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new SpaFallbackResolver());
	}

	private static class SpaFallbackResolver extends PathResourceResolver {

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			Resource requested = location.createRelative(resourcePath);
			if (requested.exists() && requested.isReadable()) {
				return requested;
			}
			for (String prefix : BACKEND_PREFIXES) {
				if (resourcePath.startsWith(prefix)) {
					return null;
				}
			}
			// Unknown client-side routes fall back to index.html so React Router can handle them.
			Resource index = new ClassPathResource("/static/index.html");
			return index.exists() ? index : null;
		}

	}

}
