package com.edstem.interviewprep.order.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record PlaceOrderRequest(
		@NotEmpty @Size(max = 100) List<@Valid OrderLineRequest> items) {
}
