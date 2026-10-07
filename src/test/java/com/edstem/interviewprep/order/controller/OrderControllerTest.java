package com.edstem.interviewprep.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.order.entity.CustomerOrder;
import com.edstem.interviewprep.order.entity.OrderItem;
import com.edstem.interviewprep.order.exception.IdempotencyKeyReusedException;
import com.edstem.interviewprep.order.exception.InsufficientStockException;
import com.edstem.interviewprep.order.exception.OrderNotFoundException;
import com.edstem.interviewprep.order.service.OrderLine;
import com.edstem.interviewprep.order.service.OrderService;
import com.edstem.interviewprep.order.service.PlacedOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerTest {

	private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OrderService service;

	@Test
	void placeReturnsCreatedForANewOrder() throws Exception {
		CustomerOrder order = new CustomerOrder("key", "hash", List.of(new OrderItem(PRODUCT_ID, 2)));
		when(service.place("key", List.of(new OrderLine(PRODUCT_ID, 2)))).thenReturn(new PlacedOrder(order, false));

		mockMvc.perform(post("/api/v1/orders")
						.header("Idempotency-Key", "key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(PRODUCT_ID, 2)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PLACED"))
				.andExpect(jsonPath("$.items[0].productId").value(PRODUCT_ID.toString()))
				.andExpect(jsonPath("$.items[0].quantity").value(2));
	}

	@Test
	void placeReturnsOkAndMarksTheResponseAsReplayedForARetry() throws Exception {
		CustomerOrder order = new CustomerOrder("key", "hash", List.of(new OrderItem(PRODUCT_ID, 2)));
		when(service.place(eq("key"), any())).thenReturn(new PlacedOrder(order, true));

		mockMvc.perform(post("/api/v1/orders")
						.header("Idempotency-Key", "key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(PRODUCT_ID, 2)))
				.andExpect(status().isOk())
				.andExpect(header().string("Idempotent-Replayed", "true"));
	}

	@Test
	void placeReturnsConflictWhenStockIsInsufficient() throws Exception {
		when(service.place(eq("key"), any())).thenThrow(new InsufficientStockException(PRODUCT_ID, 2));

		mockMvc.perform(post("/api/v1/orders")
						.header("Idempotency-Key", "key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(PRODUCT_ID, 2)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"))
				.andExpect(jsonPath("$.message").value("Insufficient stock for product " + PRODUCT_ID + ": requested 2"));
	}

	@Test
	void placeReturnsUnprocessableWhenTheKeyWasUsedForADifferentRequest() throws Exception {
		when(service.place(eq("key"), any())).thenThrow(new IdempotencyKeyReusedException("key"));

		mockMvc.perform(post("/api/v1/orders")
						.header("Idempotency-Key", "key")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(PRODUCT_ID, 2)))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSED"));
	}

	@Test
	void placeRejectsARequestWithoutAnIdempotencyKey() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body(PRODUCT_ID, 2)))
				.andExpect(status().isBadRequest());

		verify(service, never()).place(any(), any());
	}

	@Test
	void placeRejectsInvalidItemsWithFieldMessages() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
						.header("Idempotency-Key", "key")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[{\"productId\":null,\"quantity\":0}]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.length()").value(2));

		mockMvc.perform(post("/api/v1/orders")
						.header("Idempotency-Key", "key")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("items"));

		verify(service, never()).place(any(), any());
	}

	@Test
	void getReturnsTheOrder() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.get(id)).thenReturn(new CustomerOrder("key", "hash", List.of(new OrderItem(PRODUCT_ID, 1))));

		mockMvc.perform(get("/api/v1/orders/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PLACED"));
	}

	@Test
	void getReturnsNotFoundForAnUnknownOrder() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.get(id)).thenThrow(new OrderNotFoundException(id));

		mockMvc.perform(get("/api/v1/orders/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"));
	}

	@Test
	void getRejectsAMalformedOrderId() throws Exception {
		mockMvc.perform(get("/api/v1/orders/not-a-uuid"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void cancelReturnsTheCancelledOrder() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.cancel(id)).thenReturn(new CustomerOrder("key", "hash", List.of(new OrderItem(PRODUCT_ID, 1))));

		mockMvc.perform(post("/api/v1/orders/" + id + "/cancel"))
				.andExpect(status().isOk());

		verify(service).cancel(id);
	}

	private String body(UUID productId, long quantity) {
		return "{\"items\":[{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}]}";
	}
}
