package com.javora.commerce.common.storage;

import com.javora.commerce.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class StorageException extends BusinessException {

	public StorageException(String message) {
		super(HttpStatus.SERVICE_UNAVAILABLE, message);
	}

}
