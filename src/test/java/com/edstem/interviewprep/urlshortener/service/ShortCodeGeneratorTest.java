package com.edstem.interviewprep.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

	private final ShortCodeGenerator generator = new ShortCodeGenerator();

	@Test
	void generatesUrlSafeCodesWithinTheEightCharacterLimit() {
		for (int i = 0; i < 1000; i++) {
			String code = generator.generate();

			assertThat(code).hasSize(ShortCodeGenerator.CODE_LENGTH).matches("[0-9A-Za-z]+");
			assertThat(code.length()).isLessThanOrEqualTo(8);
		}
	}

	@Test
	void generatesDifferentCodes() {
		Set<String> codes = new HashSet<>();

		for (int i = 0; i < 1000; i++) {
			codes.add(generator.generate());
		}

		assertThat(codes).hasSize(1000);
	}
}
