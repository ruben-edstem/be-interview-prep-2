package com.edstem.interviewprep.order.exception;

import java.util.UUID;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InventoryItemNotFoundException extends ApiException {

	public InventoryItemNotFoundException(UUID id) {
		super(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Product " + id + " was not found");
	}
}
