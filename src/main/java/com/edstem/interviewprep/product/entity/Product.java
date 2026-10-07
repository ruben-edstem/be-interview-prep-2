package com.edstem.interviewprep.product.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "products", indexes = {
		@Index(name = "idx_products_category", columnList = "category"),
		@Index(name = "idx_products_price_cents", columnList = "priceCents")
})
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 150)
	private String name;

	@Column(nullable = false, length = 50)
	private String category;

	@Column(nullable = false)
	private long priceCents;

	@Column(nullable = false)
	private int stock;

	@Column(nullable = false)
	private double rating;

	@CreationTimestamp
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Product() {
	}

	public Product(String name, String category, long priceCents, int stock, double rating) {
		this.name = name;
		this.category = category;
		this.priceCents = priceCents;
		this.stock = stock;
		this.rating = rating;
	}

	public void update(String name, String category, long priceCents, int stock, double rating) {
		this.name = name;
		this.category = category;
		this.priceCents = priceCents;
		this.stock = stock;
		this.rating = rating;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public long getPriceCents() {
		return priceCents;
	}

	public int getStock() {
		return stock;
	}

	public double getRating() {
		return rating;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
