package com.edstem.interviewprep.auth.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration expiry) {

	private static final int MIN_SECRET_BYTES = 32;

	public JwtProperties {
		if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalArgumentException("app.jwt.secret must be at least 32 bytes long");
		}
		if (expiry == null || expiry.isNegative() || expiry.isZero()) {
			throw new IllegalArgumentException("app.jwt.expiry must be a positive duration");
		}
	}
}
