package com.edstem.interviewprep.auth.service;

import com.edstem.interviewprep.auth.dto.LoginRequest;
import com.edstem.interviewprep.auth.dto.RegisterRequest;
import com.edstem.interviewprep.auth.dto.TokenResponse;
import com.edstem.interviewprep.auth.dto.UserResponse;
import com.edstem.interviewprep.auth.entity.Role;
import com.edstem.interviewprep.auth.entity.User;
import com.edstem.interviewprep.auth.exception.EmailAlreadyRegisteredException;
import com.edstem.interviewprep.auth.exception.InvalidCredentialsException;
import com.edstem.interviewprep.auth.repository.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final String unknownUserHash;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.unknownUserHash = passwordEncoder.encode("unknown-user-placeholder");
	}

	public UserResponse register(RegisterRequest request) {
		String email = normalise(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new EmailAlreadyRegisteredException();
		}

		User user = new User(email, passwordEncoder.encode(request.password()), Role.USER);
		try {
			return UserResponse.from(userRepository.saveAndFlush(user));
		} catch (DataIntegrityViolationException ex) {
			throw new EmailAlreadyRegisteredException();
		}
	}

	public TokenResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(normalise(request.email())).orElse(null);
		String hashToCheck = user == null ? unknownUserHash : user.getPasswordHash();
		boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

		if (user == null || !passwordMatches) {
			throw new InvalidCredentialsException();
		}
		return tokenService.issueFor(user);
	}

	private String normalise(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
