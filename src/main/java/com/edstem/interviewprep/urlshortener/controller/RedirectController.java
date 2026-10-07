package com.edstem.interviewprep.urlshortener.controller;

import java.net.URI;

import com.edstem.interviewprep.urlshortener.service.ShortUrlService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final ShortUrlService service;

	public RedirectController(ShortUrlService service) {
		this.service = service;
	}

	@GetMapping("/{code:[0-9A-Za-z]{1,8}}")
	public ResponseEntity<Void> redirect(@PathVariable String code) {
		String originalUrl = service.visit(code);
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(URI.create(originalUrl))
				.cacheControl(CacheControl.noStore())
				.build();
	}
}
