package com.edstem.interviewprep.order.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderLineRequest(
		@NotNull UUID productId,
		@Min(1) long quantity) {
}
