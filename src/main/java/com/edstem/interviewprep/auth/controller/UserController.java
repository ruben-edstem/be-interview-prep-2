package com.edstem.interviewprep.auth.controller;

import com.edstem.interviewprep.auth.dto.UserResponse;
import com.edstem.interviewprep.auth.service.UserService;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return userService.getProfile(UUID.fromString(jwt.getSubject()));
	}

	@GetMapping
	public PagedModel<UserResponse> list(@PageableDefault(size = 20) Pageable pageable) {
		return new PagedModel<>(userService.listUsers(pageable));
	}
}
