package com.edstem.interviewprep.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.auth.entity.Role;
import com.edstem.interviewprep.auth.entity.User;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class UserApiTest extends AbstractAuthApiTest {

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void anyLoggedInUserCanViewTheirOwnProfile() throws Exception {
		String email = uniqueEmail();
		User user = saveUser(email, Role.USER);
		String token = loginAndGetToken(email);

		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(user.getId().toString()))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(header().doesNotExist("Set-Cookie"));
	}

	@Test
	void anAdminCanViewTheirOwnProfileToo() throws Exception {
		String email = uniqueEmail();
		saveUser(email, Role.ADMIN);
		String token = loginAndGetToken(email);

		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void requestWithoutATokenGets401AsJson() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.path").value("/api/v1/users/me"));
	}

	@Test
	void requestWithAGarbageTokenGets401AsJson() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer("not.a.jwt")))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void anExpiredTokenGets401() throws Exception {
		User user = saveUser(uniqueEmail(), Role.USER);
		String expiredToken = tokenFor(user, Instant.now().minus(Duration.ofMinutes(16)), Duration.ofMinutes(15));

		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(expiredToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void aTokenForAnAccountThatNoLongerExistsGets401() throws Exception {
		String email = uniqueEmail();
		User user = saveUser(email, Role.USER);
		String token = loginAndGetToken(email);
		userRepository.delete(user);

		mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void aUserCannotAccessTheAdminEndpoint() throws Exception {
		String email = uniqueEmail();
		saveUser(email, Role.USER);
		String token = loginAndGetToken(email);

		mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.error").value("FORBIDDEN"))
				.andExpect(jsonPath("$.path").value("/api/v1/users"));
	}

	@Test
	void adminEndpointWithoutATokenGets401NotAnHtmlPage() throws Exception {
		mockMvc.perform(get("/api/v1/users"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
	}

	@Test
	void anAdminCanListAllUsers() throws Exception {
		String adminEmail = uniqueEmail();
		String userEmail = uniqueEmail();
		saveUser(adminEmail, Role.ADMIN);
		saveUser(userEmail, Role.USER);
		String token = loginAndGetToken(adminEmail);

		mockMvc.perform(get("/api/v1/users").param("size", "100").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].email", hasItem(adminEmail)))
				.andExpect(jsonPath("$.content[*].email", hasItem(userEmail)))
				.andExpect(jsonPath("$.content[*].passwordHash").doesNotExist())
				.andExpect(jsonPath("$.page.size").value(100));
	}

	@Test
	void listCapsThePageSizeAt100() throws Exception {
		String adminEmail = uniqueEmail();
		saveUser(adminEmail, Role.ADMIN);
		String token = loginAndGetToken(adminEmail);

		mockMvc.perform(get("/api/v1/users").param("size", "5000").header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.size").value(100));
	}

	@Test
	void aTokenCannotBePromotedToAdminByChangingItsRoleClaim() throws Exception {
		String email = uniqueEmail();
		saveUser(email, Role.USER);
		String token = loginAndGetToken(email);
		String[] parts = token.split("\\.");
		String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
		String forgedPayload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.replace("\"USER\"", "\"ADMIN\"").getBytes(StandardCharsets.UTF_8));
		String forgedToken = parts[0] + "." + forgedPayload + "." + parts[2];

		assertThat(payload).contains("\"USER\"");

		mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, bearer(forgedToken)))
				.andExpect(status().isUnauthorized());
	}

	private String tokenFor(User user, Instant issuedAt, Duration lifetime) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(user.getId().toString())
				.claim("role", user.getRole().name())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(lifetime))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
