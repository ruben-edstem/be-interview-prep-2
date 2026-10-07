package com.edstem.interviewprep.order.controller;

import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.order.dto.request.PlaceOrderRequest;
import com.edstem.interviewprep.order.dto.response.OrderResponse;
import com.edstem.interviewprep.order.service.OrderLine;
import com.edstem.interviewprep.order.service.OrderService;
import com.edstem.interviewprep.order.service.PlacedOrder;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
	static final String REPLAYED_HEADER = "Idempotent-Replayed";

	private final OrderService service;

	public OrderController(OrderService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<OrderResponse> place(
			@RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
			@Valid @RequestBody PlaceOrderRequest request) {
		List<OrderLine> lines = request.items().stream()
				.map(item -> new OrderLine(item.productId(), item.quantity()))
				.toList();

		PlacedOrder placed = service.place(idempotencyKey, lines);

		OrderResponse body = OrderResponse.from(placed.order());
		if (placed.replayed()) {
			return ResponseEntity.ok().header(REPLAYED_HEADER, "true").body(body);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@GetMapping("/{id}")
	public OrderResponse get(@PathVariable UUID id) {
		return OrderResponse.from(service.get(id));
	}

	@PostMapping("/{id}/cancel")
	public OrderResponse cancel(@PathVariable UUID id) {
		return OrderResponse.from(service.cancel(id));
	}
}
