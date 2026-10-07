package com.edstem.interviewprep.urlshortener.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

@Documented
@Constraint(validatedBy = HttpUrlValidator.class)
@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface HttpUrl {

	String message() default "must be a valid http or https URL";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
