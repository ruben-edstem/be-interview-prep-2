package com.edstem.interviewprep.urlshortener.controller;

import com.edstem.interviewprep.urlshortener.dto.request.CreateShortUrlRequest;
import com.edstem.interviewprep.urlshortener.dto.response.ShortUrlResponse;
import com.edstem.interviewprep.urlshortener.entity.ShortUrl;
import com.edstem.interviewprep.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

	private final ShortUrlService service;

	public ShortUrlController(ShortUrlService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ShortUrlResponse create(@Valid @RequestBody CreateShortUrlRequest request) {
		ShortUrl created = service.create(request.url(), request.expiresAt());
		String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		return ShortUrlResponse.of(created, baseUrl);
	}
}
