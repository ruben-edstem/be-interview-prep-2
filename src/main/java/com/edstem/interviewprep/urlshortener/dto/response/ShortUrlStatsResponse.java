package com.edstem.interviewprep.urlshortener.dto.response;

import java.time.Instant;

import com.edstem.interviewprep.urlshortener.entity.ShortUrl;

public record ShortUrlStatsResponse(
		String code,
		String originalUrl,
		long visitCount,
		Instant createdAt,
		Instant expiresAt) {

	public static ShortUrlStatsResponse from(ShortUrl shortUrl) {
		return new ShortUrlStatsResponse(
				shortUrl.getCode(),
				shortUrl.getOriginalUrl(),
				shortUrl.getVisitCount(),
				shortUrl.getCreatedAt(),
				shortUrl.getExpiresAt());
	}
}
