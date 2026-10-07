package com.edstem.interviewprep.urlshortener.exception;

import org.springframework.http.HttpStatus;

public abstract class UrlShortenerException extends RuntimeException {

	private final HttpStatus status;
	private final String errorCode;

	protected UrlShortenerException(HttpStatus status, String errorCode, String message) {
		super(message);
		this.status = status;
		this.errorCode = errorCode;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
