package com.edstem.interviewprep.urlshortener.dto.response;

import java.time.Instant;

import com.edstem.interviewprep.urlshortener.entity.ShortUrl;

public record ShortUrlResponse(String code, String shortUrl, String originalUrl, Instant expiresAt) {

	public static ShortUrlResponse of(ShortUrl shortUrl, String baseUrl) {
		return new ShortUrlResponse(
				shortUrl.getCode(),
				baseUrl + "/" + shortUrl.getCode(),
				shortUrl.getOriginalUrl(),
				shortUrl.getExpiresAt());
	}
}
