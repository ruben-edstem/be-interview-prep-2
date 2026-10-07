package com.edstem.interviewprep.auth.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApiException {

	public EmailAlreadyRegisteredException() {
		super(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "Email is already registered");
	}
}
