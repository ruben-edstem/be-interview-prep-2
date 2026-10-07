package com.edstem.interviewprep.auth;

import java.util.List;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

public class RandomJwtSecretContextCustomizerFactory implements ContextCustomizerFactory {

	private static final String SECRET = TestSecrets.randomJwtSecret();

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass,
			List<ContextConfigurationAttributes> configAttributes) {
		return new RandomJwtSecretCustomizer();
	}

	private static final class RandomJwtSecretCustomizer implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
			TestPropertyValues.of("app.jwt.secret=" + SECRET).applyTo(context);
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof RandomJwtSecretCustomizer;
		}

		@Override
		public int hashCode() {
			return RandomJwtSecretCustomizer.class.hashCode();
		}
	}
}
