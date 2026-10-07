package com.edstem.interviewprep.order.entity;

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
@Table(name = "inventory_items")
public class InventoryItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false)
	private long stock;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected InventoryItem() {
	}

	public InventoryItem(String name, long stock) {
		this.name = name;
		this.stock = stock;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public long getStock() {
		return stock;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
