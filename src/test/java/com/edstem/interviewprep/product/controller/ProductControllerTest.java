package com.edstem.interviewprep.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.common.dto.PageResponse;
import com.edstem.interviewprep.product.dto.request.ProductFilter;
import com.edstem.interviewprep.product.dto.response.ProductResponse;
import com.edstem.interviewprep.product.exception.InvalidProductQueryException;
import com.edstem.interviewprep.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

	private static final String PRODUCTS = "/api/v1/products";

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

	private Pageable capturedPageable() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(productService).list(any(ProductFilter.class), captor.capture());

		return captor.getValue();
	}
}
