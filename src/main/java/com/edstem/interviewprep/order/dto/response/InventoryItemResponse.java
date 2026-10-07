package com.edstem.interviewprep.order.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.edstem.interviewprep.order.entity.InventoryItem;

public record InventoryItemResponse(UUID id, String name, long stock, Instant createdAt) {

	public static InventoryItemResponse from(InventoryItem item) {
		return new InventoryItemResponse(item.getId(), item.getName(), item.getStock(), item.getCreatedAt());
	}
}
