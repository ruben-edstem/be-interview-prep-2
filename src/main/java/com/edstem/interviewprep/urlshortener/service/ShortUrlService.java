package com.edstem.interviewprep.urlshortener.service;

import java.time.Clock;
import java.time.Instant;

import com.edstem.interviewprep.urlshortener.entity.ShortUrl;
import com.edstem.interviewprep.urlshortener.exception.CodeGenerationException;
import com.edstem.interviewprep.urlshortener.exception.ShortUrlExpiredException;
import com.edstem.interviewprep.urlshortener.exception.ShortUrlNotFoundException;
import com.edstem.interviewprep.urlshortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {

	private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);
	private static final int MAX_CODE_ATTEMPTS = 5;

	private final ShortUrlRepository repository;
	private final ShortCodeGenerator codeGenerator;
	private final Clock clock;

	public ShortUrlService(ShortUrlRepository repository, ShortCodeGenerator codeGenerator, Clock clock) {
		this.repository = repository;
		this.codeGenerator = codeGenerator;
		this.clock = clock;
	}

	public ShortUrl create(String originalUrl, Instant expiresAt) {
		for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
			try {
				return repository.saveAndFlush(new ShortUrl(codeGenerator.generate(), originalUrl, expiresAt));
			} catch (DataIntegrityViolationException e) {
				log.warn("Short code collision on attempt {} of {}", attempt, MAX_CODE_ATTEMPTS);
			}
		}
		throw new CodeGenerationException();
	}

	@Transactional
	public String visit(String code) {
		ShortUrl shortUrl = repository.findByCode(code).orElseThrow(() -> new ShortUrlNotFoundException(code));
		if (shortUrl.isExpiredAt(clock.instant())) {
			throw new ShortUrlExpiredException(code);
		}
		repository.incrementVisitCount(code);
		return shortUrl.getOriginalUrl();
	}
}
