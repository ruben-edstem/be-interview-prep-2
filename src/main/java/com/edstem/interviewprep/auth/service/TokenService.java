package com.edstem.interviewprep.auth.service;

import com.edstem.interviewprep.auth.config.JwtProperties;
import com.edstem.interviewprep.auth.dto.TokenResponse;
import com.edstem.interviewprep.auth.entity.User;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

	public static final String ROLE_CLAIM = "role";

	private static final String TOKEN_TYPE = "Bearer";

	private final JwtEncoder jwtEncoder;
	private final JwtProperties properties;
	private final Clock clock;

	public TokenService(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.properties = properties;
		this.clock = clock;
	}

	public TokenResponse issueFor(User user) {
		Instant issuedAt = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.claim(ROLE_CLAIM, user.getRole().name())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(properties.expiry()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

		return new TokenResponse(token, TOKEN_TYPE, properties.expiry().toSeconds());
	}
}
