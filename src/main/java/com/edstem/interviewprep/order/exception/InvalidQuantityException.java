package com.edstem.interviewprep.order.exception;

import java.util.UUID;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidQuantityException extends ApiException {

	public InvalidQuantityException(UUID productId) {
		super(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY",
				"Quantity for product " + productId + " must be positive and within the supported range");
	}
}
