package com.edstem.interviewprep.common.exception;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		@JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldViolation> fieldErrors) {

	public record FieldViolation(String field, String message) {
	}
}
