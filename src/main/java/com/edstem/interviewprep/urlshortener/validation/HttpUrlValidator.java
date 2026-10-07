package com.edstem.interviewprep.urlshortener.validation;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		try {
			URI uri = new URI(value);
			String scheme = uri.getScheme();
			boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
			return httpScheme && uri.getHost() != null && !uri.getHost().isBlank();
		} catch (URISyntaxException e) {
			return false;
		}
	}
}
