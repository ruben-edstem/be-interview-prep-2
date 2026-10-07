package com.edstem.interviewprep.order.entity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "orders")
public class CustomerOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "idempotency_key", nullable = false, unique = true, updatable = false, length = 100)
	private String idempotencyKey;

	@Column(name = "request_hash", nullable = false, updatable = false, length = 64)
	private String requestHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OrderStatus status;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "order_id", nullable = false, updatable = false)
	private List<OrderItem> items;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected CustomerOrder() {
	}

	public CustomerOrder(String idempotencyKey, String requestHash, List<OrderItem> items) {
		this.idempotencyKey = idempotencyKey;
		this.requestHash = requestHash;
		this.items = items;
		this.status = OrderStatus.PLACED;
	}

	public UUID getId() {
		return id;
	}

	public String getIdempotencyKey() {
		return idempotencyKey;
	}

	public String getRequestHash() {
		return requestHash;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public List<OrderItem> getItems() {
		return items;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
