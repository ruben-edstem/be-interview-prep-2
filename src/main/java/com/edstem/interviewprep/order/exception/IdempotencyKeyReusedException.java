package com.edstem.interviewprep.order.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class IdempotencyKeyReusedException extends ApiException {

	public IdempotencyKeyReusedException(String idempotencyKey) {
		super(HttpStatus.UNPROCESSABLE_ENTITY, "IDEMPOTENCY_KEY_REUSED",
				"Idempotency key " + idempotencyKey + " was already used with a different request");
	}
}
