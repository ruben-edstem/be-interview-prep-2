package com.edstem.interviewprep.auth.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApiException {

	public EmailAlreadyRegisteredException() {
		super(HttpStatus.CONFLICT, "Email is already registered");
	}
}
