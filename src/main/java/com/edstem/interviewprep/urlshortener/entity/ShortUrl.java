package com.edstem.interviewprep.urlshortener.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 8)
	private String code;

	@Column(name = "original_url", nullable = false, length = 2048)
	private String originalUrl;

	@Column(name = "visit_count", nullable = false)
	private long visitCount;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ShortUrl() {
	}

	public ShortUrl(String code, String originalUrl, Instant expiresAt) {
		this.code = code;
		this.originalUrl = originalUrl;
		this.expiresAt = expiresAt;
	}

	public boolean isExpiredAt(Instant now) {
		return expiresAt != null && !expiresAt.isAfter(now);
	}

	public UUID getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getOriginalUrl() {
		return originalUrl;
	}

	public long getVisitCount() {
		return visitCount;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
