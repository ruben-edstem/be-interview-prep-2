package com.edstem.interviewprep.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edstem.interviewprep.auth.TestSecrets;
import com.edstem.interviewprep.auth.config.JwtConfig;
import com.edstem.interviewprep.auth.config.JwtProperties;
import com.edstem.interviewprep.auth.dto.TokenResponse;
import com.edstem.interviewprep.auth.entity.Role;
import com.edstem.interviewprep.auth.entity.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.test.util.ReflectionTestUtils;

class TokenServiceTest {

	private final JwtProperties properties = new JwtProperties(TestSecrets.randomJwtSecret(), Duration.ofMinutes(15));
	private final JwtConfig jwtConfig = new JwtConfig();
	private final JwtDecoder decoder = jwtConfig.jwtDecoder(properties);

	@Test
	void issuedTokenCarriesSubjectRoleAndFifteenMinuteExpiry() {
		Instant now = Instant.now();
		TokenService service = serviceAt(now);
		User user = userWithId(Role.ADMIN);

		TokenResponse response = service.issueFor(user);
		Jwt jwt = decoder.decode(response.accessToken());

		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresInSeconds()).isEqualTo(900);
		assertThat(jwt.getSubject()).isEqualTo(user.getId().toString());
		assertThat(jwt.getClaimAsString(TokenService.ROLE_CLAIM)).isEqualTo("ADMIN");
		assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(15));
	}

	@Test
	void tokenIsRejectedOnceFifteenMinutesHavePassedWithNoGraceWindow() {
		Instant sixteenMinutesAgo = Instant.now().minus(Duration.ofMinutes(15)).minusSeconds(1);
		TokenService service = serviceAt(sixteenMinutesAgo);
		TokenResponse response = service.issueFor(userWithId(Role.USER));

		assertThatThrownBy(() -> decoder.decode(response.accessToken())).isInstanceOf(JwtValidationException.class);
	}

	@Test
	void tokenSignedWithAnotherSecretIsRejected() {
		JwtProperties otherProperties = new JwtProperties(TestSecrets.randomJwtSecret(), Duration.ofMinutes(15));
		TokenService otherService = new TokenService(jwtConfig.jwtEncoder(otherProperties), otherProperties, Clock.systemUTC());
		TokenResponse response = otherService.issueFor(userWithId(Role.USER));

		assertThatThrownBy(() -> decoder.decode(response.accessToken())).isInstanceOf(RuntimeException.class);
	}

	@Test
	void shortSecretIsRefusedAtStartup() {
		assertThatThrownBy(() -> new JwtProperties("too-short", Duration.ofMinutes(15)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private TokenService serviceAt(Instant instant) {
		return new TokenService(jwtConfig.jwtEncoder(properties), properties, Clock.fixed(instant, ZoneOffset.UTC));
	}

	private User userWithId(Role role) {
		User user = new User("jane@example.com", "hash", role);
		ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
		return user;
	}
}
