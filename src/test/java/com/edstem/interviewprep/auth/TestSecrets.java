package com.edstem.interviewprep.auth;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public final class TestSecrets {

	private static final int SECRET_BYTES = 48;

	private TestSecrets() {
	}

	public static String randomJwtSecret() {
		byte[] bytes = new byte[SECRET_BYTES];
		new SecureRandom().nextBytes(bytes);
		return Base64.getEncoder().encodeToString(bytes);
	}

	public static String randomPassword() {
		return "pw-" + UUID.randomUUID();
	}
}
