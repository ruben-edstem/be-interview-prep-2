package com.edstem.interviewprep.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import com.edstem.interviewprep.urlshortener.entity.ShortUrl;
import com.edstem.interviewprep.urlshortener.exception.CodeGenerationException;
import com.edstem.interviewprep.urlshortener.exception.ShortUrlExpiredException;
import com.edstem.interviewprep.urlshortener.exception.ShortUrlNotFoundException;
import com.edstem.interviewprep.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

	@Mock
	private ShortUrlRepository repository;

	@Mock
	private ShortCodeGenerator codeGenerator;

	private final Instant now = Instant.parse("2026-10-07T10:00:00Z");

	private ShortUrlService service;

	@BeforeEach
	void setUp() {
		service = new ShortUrlService(repository, codeGenerator, Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void createStoresTheUrlUnderTheGeneratedCode() {
		Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");
		when(codeGenerator.generate()).thenReturn("abc1234");
		when(repository.saveAndFlush(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ShortUrl created = service.create("https://example.com/a", expiresAt);

		assertThat(created.getCode()).isEqualTo("abc1234");
		assertThat(created.getOriginalUrl()).isEqualTo("https://example.com/a");
		assertThat(created.getExpiresAt()).isEqualTo(expiresAt);
	}

	@Test
	void createRetriesWithANewCodeOnCollision() {
		when(codeGenerator.generate()).thenReturn("taken01", "free002");
		when(repository.saveAndFlush(any(ShortUrl.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"))
				.thenAnswer(invocation -> invocation.getArgument(0));

		ShortUrl created = service.create("https://example.com/a", null);

		assertThat(created.getCode()).isEqualTo("free002");
		verify(repository, times(2)).saveAndFlush(any(ShortUrl.class));
	}

	@Test
	void createGivesUpAfterRepeatedCollisions() {
		when(codeGenerator.generate()).thenReturn("taken01");
		when(repository.saveAndFlush(any(ShortUrl.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThrows(CodeGenerationException.class, () -> service.create("https://example.com/a", null));

		verify(repository, times(5)).saveAndFlush(any(ShortUrl.class));
	}

	@Test
	void visitCountsTheVisitAndReturnsTheOriginalUrl() {
		when(repository.findByCode("abc1234"))
				.thenReturn(Optional.of(new ShortUrl("abc1234", "https://example.com/a", null)));

		String target = service.visit("abc1234");

		assertThat(target).isEqualTo("https://example.com/a");
		verify(repository).incrementVisitCount("abc1234");
	}

	@Test
	void visitStillWorksBeforeTheExpiry() {
		when(repository.findByCode("abc1234"))
				.thenReturn(Optional.of(new ShortUrl("abc1234", "https://example.com/a", now.plusSeconds(1))));

		String target = service.visit("abc1234");

		assertThat(target).isEqualTo("https://example.com/a");
		verify(repository).incrementVisitCount("abc1234");
	}

	@Test
	void visitRejectsUnknownCodeWithoutCounting() {
		when(repository.findByCode("nothere")).thenReturn(Optional.empty());

		assertThrows(ShortUrlNotFoundException.class, () -> service.visit("nothere"));

		verify(repository, never()).incrementVisitCount(any());
	}

	@Test
	void visitRejectsExpiredCodeWithoutCounting() {
		when(repository.findByCode("old0001"))
				.thenReturn(Optional.of(new ShortUrl("old0001", "https://example.com/a", now)));

		assertThrows(ShortUrlExpiredException.class, () -> service.visit("old0001"));

		verify(repository, never()).incrementVisitCount(any());
	}

	@Test
	void getStatsReturnsTheStoredLink() {
		ShortUrl stored = new ShortUrl("abc1234", "https://example.com/a", null);
		when(repository.findByCode("abc1234")).thenReturn(Optional.of(stored));

		ShortUrl stats = service.getStats("abc1234");

		assertThat(stats).isSameAs(stored);
	}

	@Test
	void getStatsRejectsUnknownCode() {
		when(repository.findByCode("nothere")).thenReturn(Optional.empty());

		assertThrows(ShortUrlNotFoundException.class, () -> service.getStats("nothere"));
	}

	@Test
	void sameUrlSubmittedTwiceCreatesTwoIndependentLinks() {
		when(codeGenerator.generate()).thenReturn("first01", "second2");
		when(repository.saveAndFlush(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ShortUrl first = service.create("https://example.com/a", null);
		ShortUrl second = service.create("https://example.com/a", null);

		assertThat(first.getCode()).isNotEqualTo(second.getCode());
		assertThat(first.getOriginalUrl()).isEqualTo(second.getOriginalUrl());
	}
}
