package com.edstem.interviewprep.product.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.edstem.interviewprep.product.entity.Product;

public record ProductResponse(
		UUID id,
		String name,
		String category,
		long priceCents,
		int stock,
		double rating,
		Instant createdAt) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getName(),
				product.getCategory(),
				product.getPriceCents(),
				product.getStock(),
				product.getRating(),
				product.getCreatedAt());
	}
}
