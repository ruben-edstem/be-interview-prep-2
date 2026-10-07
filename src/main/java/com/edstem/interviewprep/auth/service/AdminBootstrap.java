package com.edstem.interviewprep.auth.service;

import com.edstem.interviewprep.auth.config.AdminProperties;
import com.edstem.interviewprep.auth.entity.Role;
import com.edstem.interviewprep.auth.entity.User;
import com.edstem.interviewprep.auth.repository.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(AdminProperties.class)
public class AdminBootstrap implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
	private static final int MIN_PASSWORD_LENGTH = 8;

	private final AdminProperties properties;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminBootstrap(AdminProperties properties, UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.properties = properties;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.isConfigured()) {
			log.info("No ADMIN_EMAIL and ADMIN_PASSWORD set, skipping admin bootstrap");
			return;
		}
		if (properties.password().length() < MIN_PASSWORD_LENGTH) {
			throw new IllegalStateException("app.admin.password must be at least " + MIN_PASSWORD_LENGTH + " characters");
		}

		String email = properties.email().trim().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(email)) {
			log.info("Admin account already exists, leaving it unchanged");
			return;
		}

		userRepository.save(new User(email, passwordEncoder.encode(properties.password()), Role.ADMIN));
		log.info("Created admin account");
	}
}
