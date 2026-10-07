package com.edstem.interviewprep.product.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductRequest(
		@NotBlank(message = "Name is required")
		@Size(max = 150, message = "Name must be at most 150 characters")
		String name,

		@NotBlank(message = "Category is required")
		@Size(max = 50, message = "Category must be at most 50 characters")
		String category,

		@NotNull(message = "Price is required")
		@Positive(message = "Price must be greater than zero")
		Long priceCents,

		@NotNull(message = "Stock is required")
		@PositiveOrZero(message = "Stock cannot be negative")
		Integer stock,

		@NotNull(message = "Rating is required")
		@DecimalMin(value = "0.0", message = "Rating must be between 0 and 5")
		@DecimalMax(value = "5.0", message = "Rating must be between 0 and 5")
		Double rating) {

	public ProductRequest {
		name = name == null ? null : name.trim();
		category = category == null ? null : category.trim();
	}
}
