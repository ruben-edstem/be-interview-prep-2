package com.edstem.interviewprep.common.exception;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.common.exception.ErrorResponse.FieldViolation;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static final String VALIDATION_FAILED = "VALIDATION_FAILED";
	private static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
	private static final String INVALID_PARAMETER = "INVALID_PARAMETER";
	private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Object> handleApiException(ApiException ex, HttpServletRequest request) {
		return respond(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {}", request.getRequestURI(), ex);

		return respond(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "An unexpected error occurred",
				request.getRequestURI(), List.of());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
				.sorted(Comparator.comparing(FieldViolation::field))
				.toList();

		return respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, "Validation failed", path(request), violations);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (ex.getCause() instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
			String field = invalid.getPath().get(invalid.getPath().size() - 1).getFieldName();
			FieldViolation violation = new FieldViolation(field, describeInvalidValue(invalid.getTargetType()));

			return respond(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, "Validation failed", path(request),
					List.of(violation));
		}

		return respond(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST, "Malformed JSON request", path(request), List.of());
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String name = ex instanceof MethodArgumentTypeMismatchException mismatch
				? mismatch.getName()
				: ex.getPropertyName();
		FieldViolation violation = new FieldViolation(name, describeInvalidValue(ex.getRequiredType()));

		return respond(HttpStatus.BAD_REQUEST, INVALID_PARAMETER, "Invalid request parameter", path(request),
				List.of(violation));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		HttpStatus status = HttpStatus.valueOf(statusCode.value());
		String message = body instanceof ProblemDetail problem && problem.getDetail() != null
				? problem.getDetail()
				: status.getReasonPhrase();

		return respond(status, status.name(), message, path(request), List.of());
	}

	private ResponseEntity<Object> respond(HttpStatus status, String code, String message, String path,
			List<FieldViolation> violations) {
		ErrorResponse body = new ErrorResponse(Instant.now(), status.value(), code, message, path, violations);

		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
	}

	private String path(WebRequest request) {
		return ((ServletWebRequest) request).getRequest().getRequestURI();
	}

	private String describeInvalidValue(Class<?> targetType) {
		if (targetType != null && targetType.isEnum()) {
			return "Must be one of " + Arrays.toString(targetType.getEnumConstants());
		}
		if (targetType != null && LocalDate.class.isAssignableFrom(targetType)) {
			return "Must be a valid date in yyyy-MM-dd format";
		}
		if (targetType != null && UUID.class.isAssignableFrom(targetType)) {
			return "Must be a valid UUID";
		}

		return "Invalid value";
	}
}
