package com.edstem.interviewprep.order.controller;

import java.util.UUID;

import com.edstem.interviewprep.order.dto.request.CreateInventoryItemRequest;
import com.edstem.interviewprep.order.dto.response.InventoryItemResponse;
import com.edstem.interviewprep.order.service.InventoryItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory-items")
public class InventoryItemController {

	private final InventoryItemService service;

	public InventoryItemController(InventoryItemService service) {
		this.service = service;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InventoryItemResponse create(@Valid @RequestBody CreateInventoryItemRequest request) {
		return InventoryItemResponse.from(service.create(request.name(), request.stock()));
	}

	@GetMapping("/{id}")
	public InventoryItemResponse get(@PathVariable UUID id) {
		return InventoryItemResponse.from(service.get(id));
	}
}
