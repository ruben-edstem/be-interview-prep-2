package com.edstem.interviewprep.order.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "product_id", nullable = false, updatable = false)
	private UUID productId;

	@Column(nullable = false, updatable = false)
	private long quantity;

	protected OrderItem() {
	}

	public OrderItem(UUID productId, long quantity) {
		this.productId = productId;
		this.quantity = quantity;
	}

	public UUID getProductId() {
		return productId;
	}

	public long getQuantity() {
		return quantity;
	}
}
