package com.edstem.interviewprep.urlshortener.service;

import java.time.Instant;

import com.edstem.interviewprep.urlshortener.entity.ShortUrl;
import com.edstem.interviewprep.urlshortener.exception.CodeGenerationException;
import com.edstem.interviewprep.urlshortener.repository.ShortUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ShortUrlService {

	private static final Logger log = LoggerFactory.getLogger(ShortUrlService.class);
	private static final int MAX_CODE_ATTEMPTS = 5;

	private final ShortUrlRepository repository;
	private final ShortCodeGenerator codeGenerator;

	public ShortUrlService(ShortUrlRepository repository, ShortCodeGenerator codeGenerator) {
		this.repository = repository;
		this.codeGenerator = codeGenerator;
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
}
