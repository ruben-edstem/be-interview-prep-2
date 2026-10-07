package com.edstem.interviewprep.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.edstem.interviewprep.auth.entity.Role;
import com.edstem.interviewprep.auth.entity.User;
import com.edstem.interviewprep.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractAuthApiTest {

	protected static final String PASSWORD = "correct-horse-battery";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	protected String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	protected User saveUser(String email, Role role) {
		return userRepository.save(new User(email, passwordEncoder.encode(PASSWORD), role));
	}

	protected String loginAndGetToken(String email) throws Exception {
		String body = mockMvc.perform(post("/api/v1/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("email", email, "password", PASSWORD))))
				.andReturn().getResponse().getContentAsString();
		JsonNode json = objectMapper.readTree(body);

		return json.get("accessToken").asText();
	}

	protected String bearer(String token) {
		return "Bearer " + token;
	}
}
