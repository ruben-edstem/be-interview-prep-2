package com.edstem.interviewprep.urlshortener.dto.request;

import java.time.Instant;

import com.edstem.interviewprep.urlshortener.validation.HttpUrl;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateShortUrlRequest(
		@NotBlank @Size(max = 2048) @HttpUrl String url,
		@Future Instant expiresAt) {
}
