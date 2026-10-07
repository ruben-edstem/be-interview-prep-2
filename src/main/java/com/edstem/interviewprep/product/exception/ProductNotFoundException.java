package com.edstem.interviewprep.product.exception;

import java.util.UUID;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {

	public ProductNotFoundException(UUID id) {
		super(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product not found: " + id);
	}
}
