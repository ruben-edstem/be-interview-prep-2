package com.edstem.interviewprep.product.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidProductQueryException extends ApiException {

	public InvalidProductQueryException(String message) {
		super(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_QUERY", message);
	}
}
