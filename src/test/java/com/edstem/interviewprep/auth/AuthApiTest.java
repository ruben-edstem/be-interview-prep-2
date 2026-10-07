package com.edstem.interviewprep.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.auth.entity.Role;
import com.edstem.interviewprep.auth.entity.User;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AuthApiTest extends AbstractAuthApiTest {

	@Test
	void registerCreatesAUserWithTheUserRoleAndAHashedPassword() throws Exception {
		String email = uniqueEmail();

		mockMvc.perform(register(email, PASSWORD))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());

		User stored = userRepository.findByEmail(email).orElseThrow();
		assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
		assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
	}

	@Test
	void registerCannotGrantItselfTheAdminRole() throws Exception {
		String email = uniqueEmail();
		String body = objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD, "role", "ADMIN"));

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("USER"));

		assertThat(userRepository.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.USER);
	}

	@Test
	void registerRejectsADuplicateEmailEvenWithDifferentCase() throws Exception {
		String email = uniqueEmail();
		mockMvc.perform(register(email, PASSWORD)).andExpect(status().isCreated());

		mockMvc.perform(register(email.toUpperCase(), PASSWORD))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("Email is already registered"));
	}

	@Test
	void registerReturnsFieldLevelMessagesForInvalidInput() throws Exception {
		mockMvc.perform(register("not-an-email", "short"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.fieldErrors.email").value("Email must be a valid address"))
				.andExpect(jsonPath("$.fieldErrors.password").value("Password must be between 8 and 72 characters"));
	}

	@Test
	void loginReturnsABearerTokenThatExpiresInFifteenMinutes() throws Exception {
		String email = uniqueEmail();
		saveUser(email, Role.USER);

		mockMvc.perform(login(email, PASSWORD))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").value(900))
				.andExpect(header().doesNotExist("Set-Cookie"));
	}

	@Test
	void loginRejectsAWrongPasswordAndAnUnknownEmailWithTheSameResponse() throws Exception {
		String email = uniqueEmail();
		saveUser(email, Role.USER);

		mockMvc.perform(login(email, "wrong-password-123"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
		mockMvc.perform(login(uniqueEmail(), PASSWORD))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid email or password"));
	}

	@Test
	void loginRequiresBothFields() throws Exception {
		mockMvc.perform(login("", ""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.email").value("Email is required"))
				.andExpect(jsonPath("$.fieldErrors.password").value("Password is required"));
	}

	@Test
	void errorResponsesNeverContainTheSubmittedPassword() throws Exception {
		mockMvc.perform(login(uniqueEmail(), "super-secret-attempt"))
				.andExpect(content().string(not(containsString("super-secret-attempt"))));
	}

	private MockHttpServletRequestBuilder register(String email, String password) throws Exception {
		return post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)));
	}

	private MockHttpServletRequestBuilder login(String email, String password) throws Exception {
		return post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)));
	}
}
