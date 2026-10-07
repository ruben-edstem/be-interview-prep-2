package com.edstem.interviewprep.order.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.edstem.interviewprep.order.entity.InventoryItem;
import com.edstem.interviewprep.order.exception.InventoryItemNotFoundException;
import com.edstem.interviewprep.order.service.InventoryItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class InventoryItemControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private InventoryItemService service;

	@Test
	void createReturnsTheStoredItem() throws Exception {
		when(service.create("Widget", 10)).thenReturn(new InventoryItem("Widget", 10));

		mockMvc.perform(post("/api/v1/inventory-items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Widget\",\"stock\":10}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Widget"))
				.andExpect(jsonPath("$.stock").value(10));
	}

	@Test
	void createRejectsABlankNameAndNegativeStock() throws Exception {
		mockMvc.perform(post("/api/v1/inventory-items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\" \",\"stock\":-1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.length()").value(2));

		verify(service, never()).create(any(), anyLong());
	}

	@Test
	void getReturnsNotFoundForAnUnknownItem() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.get(id)).thenThrow(new InventoryItemNotFoundException(id));

		mockMvc.perform(get("/api/v1/inventory-items/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
	}
}
