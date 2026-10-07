package com.edstem.interviewprep;

import com.edstem.interviewprep.auth.TestSecrets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class InterviewPrepApplicationTests {

	@DynamicPropertySource
	static void jwtSecret(DynamicPropertyRegistry registry) {
		registry.add("app.jwt.secret", TestSecrets::randomJwtSecret);
	}

	@Test
	void contextLoads() {
	}

}
