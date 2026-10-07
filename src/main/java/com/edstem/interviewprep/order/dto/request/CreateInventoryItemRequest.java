package com.edstem.interviewprep.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateInventoryItemRequest(
		@NotBlank @Size(max = 200) String name,
		@Min(0) long stock) {
}
