package com.edstem.interviewprep.auth.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fieldErrors) {

	public static ApiError of(HttpStatus status, String message) {
		return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, null);
	}

	public static ApiError of(HttpStatus status, String message, Map<String, String> fieldErrors) {
		return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, fieldErrors);
	}
}
