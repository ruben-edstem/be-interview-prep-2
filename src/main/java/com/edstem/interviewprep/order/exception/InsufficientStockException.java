package com.edstem.interviewprep.order.exception;

import java.util.UUID;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ApiException {

	public InsufficientStockException(UUID productId, long requested) {
		super(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK",
				"Insufficient stock for product " + productId + ": requested " + requested);
	}
}
