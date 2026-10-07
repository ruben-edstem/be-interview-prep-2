package com.edstem.interviewprep.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.common.dto.PageResponse;
import com.edstem.interviewprep.product.dto.request.ProductFilter;
import com.edstem.interviewprep.product.dto.request.ProductRequest;
import com.edstem.interviewprep.product.dto.response.ProductResponse;
import com.edstem.interviewprep.product.exception.InvalidProductQueryException;
import com.edstem.interviewprep.product.exception.ProductNotFoundException;
import com.edstem.interviewprep.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	private static final String PRODUCTS = "/api/v1/products";
	private static final String VALID_BODY =
			"{\"name\":\"Desk\",\"category\":\"Home\",\"priceCents\":4999,\"stock\":3,\"rating\":4.5}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@Test
	void listReturnsPageWithTotals() throws Exception {
		ProductResponse product = new ProductResponse(UUID.randomUUID(), "Desk", "Home", 4_999, 3, 4.5, Instant.now());
		when(productService.list(any(ProductFilter.class), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(product), 0, 20, 41, 3));

		mockMvc.perform(get(PRODUCTS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].name").value("Desk"))
				.andExpect(jsonPath("$.content[0].priceCents").value(4_999))
				.andExpect(jsonPath("$.totalElements").value(41))
				.andExpect(jsonPath("$.totalPages").value(3));
	}

	@Test
	void listPassesPageAndSortToService() throws Exception {
		when(productService.list(any(ProductFilter.class), any(Pageable.class))).thenReturn(new PageResponse<>(List.of(), 2, 5, 0, 0));

		mockMvc.perform(get(PRODUCTS).param("page", "2").param("size", "5").param("sort", "priceCents,desc"))
				.andExpect(status().isOk());

		Pageable pageable = capturedPageable();
		assertThat(pageable.getPageNumber()).isEqualTo(2);
		assertThat(pageable.getPageSize()).isEqualTo(5);
		assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("priceCents"));
	}

	@Test
	void listCapsPageSizeAtOneHundred() throws Exception {
		when(productService.list(any(ProductFilter.class), any(Pageable.class))).thenReturn(new PageResponse<>(List.of(), 0, 100, 0, 0));

		mockMvc.perform(get(PRODUCTS).param("size", "500")).andExpect(status().isOk());

		assertThat(capturedPageable().getPageSize()).isEqualTo(100);
	}

	@Test
	void listReturns400WhenSortFieldIsRejected() throws Exception {
		when(productService.list(any(ProductFilter.class), any(Pageable.class)))
				.thenThrow(new InvalidProductQueryException("Cannot sort by 'password'"));

		mockMvc.perform(get(PRODUCTS).param("sort", "password"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PRODUCT_QUERY"))
				.andExpect(jsonPath("$.message").value("Cannot sort by 'password'"));
	}

	@Test
	void listBindsAllFiltersIntoOneFilterObject() throws Exception {
		when(productService.list(any(ProductFilter.class), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get(PRODUCTS)
						.param("category", "Books")
						.param("minPriceCents", "500")
						.param("maxPriceCents", "2500")
						.param("inStock", "true")
						.param("search", "novel"))
				.andExpect(status().isOk());

		ArgumentCaptor<ProductFilter> captor = ArgumentCaptor.forClass(ProductFilter.class);
		verify(productService).list(captor.capture(), any(Pageable.class));
		assertThat(captor.getValue()).isEqualTo(new ProductFilter("Books", 500L, 2_500L, true, "novel"));
	}

	@Test
	void listWithoutFiltersPassesEmptyFilter() throws Exception {
		when(productService.list(any(ProductFilter.class), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get(PRODUCTS)).andExpect(status().isOk());

		ArgumentCaptor<ProductFilter> captor = ArgumentCaptor.forClass(ProductFilter.class);
		verify(productService).list(captor.capture(), any(Pageable.class));
		assertThat(captor.getValue()).isEqualTo(ProductFilter.none());
	}

	@Test
	void listReturns400WhenPriceFilterIsNotANumber() throws Exception {
		mockMvc.perform(get(PRODUCTS).param("minPriceCents", "cheap"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PARAMETER"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("minPriceCents"));
	}

	@Test
	void getReturnsProduct() throws Exception {
		UUID id = UUID.randomUUID();
		when(productService.get(id)).thenReturn(response(id, "Desk"));

		mockMvc.perform(get(PRODUCTS + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.name").value("Desk"));
	}

	@Test
	void getReturns404ForUnknownProduct() throws Exception {
		UUID id = UUID.randomUUID();
		when(productService.get(id)).thenThrow(new ProductNotFoundException(id));

		mockMvc.perform(get(PRODUCTS + "/" + id))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void getReturns400ForMalformedId() throws Exception {
		mockMvc.perform(get(PRODUCTS + "/not-a-uuid"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PARAMETER"));
	}

	@Test
	void createReturns201WithLocation() throws Exception {
		UUID id = UUID.randomUUID();
		when(productService.create(any(ProductRequest.class))).thenReturn(response(id, "Desk"));

		mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost" + PRODUCTS + "/" + id))
				.andExpect(jsonPath("$.name").value("Desk"));
	}

	@Test
	void createReturnsFieldErrorsForInvalidInput() throws Exception {
		String body = "{\"name\":\" \",\"category\":\"Home\",\"priceCents\":0,\"stock\":-1,\"rating\":9}";

		mockMvc.perform(post(PRODUCTS).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='name')].message").value("Name is required"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='priceCents')].message")
						.value("Price must be greater than zero"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='stock')].message").value("Stock cannot be negative"))
				.andExpect(jsonPath("$.fieldErrors[?(@.field=='rating')].message")
						.value("Rating must be between 0 and 5"));
		verifyNoInteractions(productService);
	}

	@Test
	void updateReturnsUpdatedProduct() throws Exception {
		UUID id = UUID.randomUUID();
		when(productService.update(eq(id), any(ProductRequest.class))).thenReturn(response(id, "Desk"));

		mockMvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Desk"));
	}

	@Test
	void updateReturns404ForUnknownProduct() throws Exception {
		UUID id = UUID.randomUUID();
		when(productService.update(eq(id), any(ProductRequest.class))).thenThrow(new ProductNotFoundException(id));

		mockMvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
				.andExpect(status().isNotFound());
	}

	@Test
	void deleteReturns204() throws Exception {
		UUID id = UUID.randomUUID();

		mockMvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNoContent());

		verify(productService).delete(id);
	}

	@Test
	void deleteReturns404ForUnknownProduct() throws Exception {
		UUID id = UUID.randomUUID();
		doThrow(new ProductNotFoundException(id)).when(productService).delete(id);

		mockMvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNotFound());
	}

	private ProductResponse response(UUID id, String name) {
		return new ProductResponse(id, name, "Home", 4_999, 3, 4.5, Instant.now());
	}

	private Pageable capturedPageable() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(productService).list(any(ProductFilter.class), captor.capture());

		return captor.getValue();
	}
}
