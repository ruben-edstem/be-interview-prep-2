package com.edstem.interviewprep.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.auth.config.AdminProperties;
import com.edstem.interviewprep.auth.entity.Role;
import com.edstem.interviewprep.auth.entity.User;
import com.edstem.interviewprep.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Test
	void createsAnAdminWithAHashedPasswordWhenConfiguredAndAbsent() {
		AdminBootstrap bootstrap = bootstrapWith(" Admin@Example.com ", "admin-password");
		when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
		when(passwordEncoder.encode("admin-password")).thenReturn("hashed");

		bootstrap.run(null);

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("admin@example.com");
		assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
	}

	@Test
	void leavesAnExistingAdminUntouched() {
		AdminBootstrap bootstrap = bootstrapWith("admin@example.com", "admin-password");
		when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

		bootstrap.run(null);

		verify(userRepository, never()).save(any());
	}

	@Test
	void doesNothingWhenNoAdminIsConfigured() {
		AdminBootstrap bootstrap = bootstrapWith("", "");

		bootstrap.run(null);

		verify(userRepository, never()).save(any());
	}

	@Test
	void refusesToStartWithAWeakAdminPassword() {
		AdminBootstrap bootstrap = bootstrapWith("admin@example.com", "short");

		assertThatThrownBy(() -> bootstrap.run(null)).isInstanceOf(IllegalStateException.class);
	}

	private AdminBootstrap bootstrapWith(String email, String password) {
		return new AdminBootstrap(new AdminProperties(email, password), userRepository, passwordEncoder);
	}
}
