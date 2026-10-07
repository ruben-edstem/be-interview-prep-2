package com.edstem.interviewprep.urlshortener.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CodeGenerationException extends ApiException {

	public CodeGenerationException() {
		super(HttpStatus.INTERNAL_SERVER_ERROR, "CODE_GENERATION_FAILED",
				"Could not generate a unique short code, please retry");
	}
}
