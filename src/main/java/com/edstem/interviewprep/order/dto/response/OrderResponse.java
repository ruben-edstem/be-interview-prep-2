package com.edstem.interviewprep.order.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.order.entity.CustomerOrder;
import com.edstem.interviewprep.order.entity.OrderItem;
import com.edstem.interviewprep.order.entity.OrderStatus;

public record OrderResponse(UUID id, OrderStatus status, List<Line> items, Instant createdAt) {

	public record Line(UUID productId, long quantity) {

		static Line from(OrderItem item) {
			return new Line(item.getProductId(), item.getQuantity());
		}
	}

	public static OrderResponse from(CustomerOrder order) {
		List<Line> lines = order.getItems().stream().map(Line::from).toList();
		return new OrderResponse(order.getId(), order.getStatus(), lines, order.getCreatedAt());
	}
}
