package com.edstem.interviewprep.auth.service;

import com.edstem.interviewprep.auth.dto.UserResponse;
import com.edstem.interviewprep.auth.exception.AccountNotFoundException;
import com.edstem.interviewprep.auth.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

	private static final Set<String> SORTABLE_FIELDS = Set.of("email", "role", "createdAt");
	private static final Sort DEFAULT_SORT = Sort.by("createdAt");

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public UserResponse getProfile(UUID userId) {
		return userRepository.findById(userId)
				.map(UserResponse::from)
				.orElseThrow(AccountNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public Page<UserResponse> listUsers(Pageable pageable) {
		return userRepository.findAll(restrictSort(pageable)).map(UserResponse::from);
	}

	private Pageable restrictSort(Pageable pageable) {
		List<Sort.Order> allowed = pageable.getSort().stream()
				.filter(order -> SORTABLE_FIELDS.contains(order.getProperty()))
				.toList();
		Sort sort = allowed.isEmpty() ? DEFAULT_SORT : Sort.by(allowed);

		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}
}
