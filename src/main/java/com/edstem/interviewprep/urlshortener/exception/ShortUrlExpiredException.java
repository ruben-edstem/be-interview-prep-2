package com.edstem.interviewprep.urlshortener.exception;

import org.springframework.http.HttpStatus;

public class ShortUrlExpiredException extends UrlShortenerException {

	public ShortUrlExpiredException(String code) {
		super(HttpStatus.GONE, "SHORT_URL_EXPIRED", "The short URL for code " + code + " has expired");
	}
}
