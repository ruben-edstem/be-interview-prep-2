package com.edstem.interviewprep.urlshortener.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import com.edstem.interviewprep.urlshortener.entity.ShortUrl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class ShortUrlRepositoryTest {

	@Autowired
	private ShortUrlRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savesWithZeroVisitsAndCreatedTimestamp() {
		ShortUrl saved = repository.saveAndFlush(new ShortUrl("abc1234", "https://example.com/a", null));

		ShortUrl found = repository.findByCode("abc1234").orElseThrow();

		assertThat(found.getId()).isEqualTo(saved.getId());
		assertThat(found.getVisitCount()).isZero();
		assertThat(found.getCreatedAt()).isNotNull();
	}

	@Test
	void rejectsDuplicateCode() {
		repository.saveAndFlush(new ShortUrl("dup0001", "https://example.com/a", null));

		ShortUrl duplicate = new ShortUrl("dup0001", "https://example.com/b", null);

		assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
	}

	@Test
	void incrementVisitCountUpdatesOnlyTheMatchingCode() {
		repository.saveAndFlush(new ShortUrl("hit0001", "https://example.com/a", null));
		repository.saveAndFlush(new ShortUrl("hit0002", "https://example.com/b", Instant.now()));

		int updated = repository.incrementVisitCount("hit0001");
		repository.incrementVisitCount("hit0001");
		entityManager.clear();

		assertThat(updated).isEqualTo(1);
		assertThat(repository.findByCode("hit0001").orElseThrow().getVisitCount()).isEqualTo(2);
		assertThat(repository.findByCode("hit0002").orElseThrow().getVisitCount()).isZero();
	}

	@Test
	void incrementVisitCountReturnsZeroForUnknownCode() {
		int updated = repository.incrementVisitCount("nothere");

		assertThat(updated).isZero();
	}
}
