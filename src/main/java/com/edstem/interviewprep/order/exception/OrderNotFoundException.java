package com.edstem.interviewprep.order.exception;

import java.util.UUID;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {

	public OrderNotFoundException(UUID id) {
		super(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order " + id + " was not found");
	}
}
