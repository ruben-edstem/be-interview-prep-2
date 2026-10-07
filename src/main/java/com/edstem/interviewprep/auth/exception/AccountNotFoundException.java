package com.edstem.interviewprep.auth.exception;

import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

	public AccountNotFoundException() {
		super(HttpStatus.UNAUTHORIZED, "The account for this token no longer exists");
	}
}
