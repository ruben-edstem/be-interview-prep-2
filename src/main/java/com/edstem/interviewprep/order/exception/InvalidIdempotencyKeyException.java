package com.edstem.interviewprep.order.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidIdempotencyKeyException extends ApiException {

	public InvalidIdempotencyKeyException(int maxLength) {
		super(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY",
				"Idempotency-Key must be between 1 and " + maxLength + " characters");
	}
}
