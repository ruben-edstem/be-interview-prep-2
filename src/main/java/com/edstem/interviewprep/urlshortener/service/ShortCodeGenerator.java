package com.edstem.interviewprep.urlshortener.service;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

	static final int CODE_LENGTH = 7;

	private static final char[] ALPHABET =
			"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

	private final SecureRandom random = new SecureRandom();

	public String generate() {
		char[] code = new char[CODE_LENGTH];
		for (int i = 0; i < CODE_LENGTH; i++) {
			code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
		}
		return new String(code);
	}
}
