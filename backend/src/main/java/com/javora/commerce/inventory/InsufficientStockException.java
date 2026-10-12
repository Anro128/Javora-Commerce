package com.javora.commerce.inventory;

import com.javora.commerce.common.exception.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InsufficientStockException extends BusinessException {

	private final Long variantId;

	public InsufficientStockException(Long variantId) {
		super(HttpStatus.CONFLICT, "Not enough stock available");
		this.variantId = variantId;
	}

}
