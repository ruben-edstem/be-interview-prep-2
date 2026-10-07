package com.edstem.interviewprep.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.auth.exception.AccountNotFoundException;
import com.edstem.interviewprep.auth.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	void getProfileFailsWhenTheAccountIsGone() {
		UUID missingId = UUID.randomUUID();
		when(userRepository.findById(missingId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getProfile(missingId)).isInstanceOf(AccountNotFoundException.class);
	}

	@Test
	void listUsersDropsASortOnThePasswordHashAndFallsBackToCreatedAt() {
		when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
		Pageable requested = PageRequest.of(0, 20, Sort.by("passwordHash"));

		userService.listUsers(requested);

		ArgumentCaptor<Pageable> used = ArgumentCaptor.forClass(Pageable.class);
		verify(userRepository).findAll(used.capture());
		assertThat(used.getValue().getSort()).isEqualTo(Sort.by("createdAt"));
	}

	@Test
	void listUsersKeepsAllowedSortFieldsAndPaging() {
		when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(java.util.List.of()));
		Pageable requested = PageRequest.of(2, 50, Sort.by(Sort.Direction.DESC, "email").and(Sort.by("passwordHash")));

		userService.listUsers(requested);

		ArgumentCaptor<Pageable> used = ArgumentCaptor.forClass(Pageable.class);
		verify(userRepository).findAll(used.capture());
		assertThat(used.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "email"));
		assertThat(used.getValue().getPageNumber()).isEqualTo(2);
		assertThat(used.getValue().getPageSize()).isEqualTo(50);
	}
}
