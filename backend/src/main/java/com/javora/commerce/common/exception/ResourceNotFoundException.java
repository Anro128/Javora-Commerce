package com.javora.commerce.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

	public ResourceNotFoundException(String resource, Object identifier) {
		super(HttpStatus.NOT_FOUND, "%s not found: %s".formatted(resource, identifier));
	}

}
