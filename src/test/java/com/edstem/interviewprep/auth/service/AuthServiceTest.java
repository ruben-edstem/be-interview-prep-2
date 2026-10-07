package com.edstem.interviewprep.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.auth.dto.RegisterRequest;
import com.edstem.interviewprep.auth.entity.User;
import com.edstem.interviewprep.auth.exception.EmailAlreadyRegisteredException;
import com.edstem.interviewprep.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private TokenService tokenService;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		when(passwordEncoder.encode(any())).thenReturn("hash");
		authService = new AuthService(userRepository, passwordEncoder, tokenService);
	}

	@Test
	void registerReportsAConflictWhenTwoRequestsRaceOnTheSameEmail() {
		RegisterRequest request = new RegisterRequest("race@example.com", "correct-horse-battery");
		when(userRepository.existsByEmail("race@example.com")).thenReturn(false);
		when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("unique"));

		assertThatThrownBy(() -> authService.register(request)).isInstanceOf(EmailAlreadyRegisteredException.class);
	}
}
