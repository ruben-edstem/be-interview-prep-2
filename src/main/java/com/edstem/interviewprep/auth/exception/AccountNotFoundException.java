package com.edstem.interviewprep.auth.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AccountNotFoundException extends ApiException {

	public AccountNotFoundException() {
		super(HttpStatus.UNAUTHORIZED, "ACCOUNT_NOT_FOUND", "The account for this token no longer exists");
	}
}
