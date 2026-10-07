package com.edstem.interviewprep.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.edstem.interviewprep.common.dto.PageResponse;
import com.edstem.interviewprep.product.dto.response.ProductResponse;
import com.edstem.interviewprep.product.entity.Product;
import com.edstem.interviewprep.product.exception.InvalidProductQueryException;
import com.edstem.interviewprep.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductService productService;

	@Test
	void listReturnsPageMetadataWithTotalCountAndPages() {
		Pageable pageable = PageRequest.of(1, 2);
		Page<Product> page = new PageImpl<>(List.of(product("Desk"), product("Lamp")), pageable, 5);
		when(productRepository.findAll(any(Pageable.class))).thenReturn(page);

		PageResponse<ProductResponse> response = productService.list(pageable);

		assertThat(response.content()).extracting(ProductResponse::name).containsExactly("Desk", "Lamp");
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(5);
		assertThat(response.totalPages()).isEqualTo(3);
	}

	@Test
	void listSortsByNameWhenNoSortRequested() {
		when(productRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

		productService.list(PageRequest.of(0, 10));

		assertThat(capturedSort()).containsExactly(Sort.Order.asc("name"), Sort.Order.asc("id"));
	}

	@Test
	void listAppendsIdAsTieBreakerSoPagesAreStable() {
		when(productRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

		productService.list(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "rating")));

		assertThat(capturedSort()).containsExactly(Sort.Order.desc("rating"), Sort.Order.asc("id"));
	}

	@Test
	void listRejectsUnknownSortField() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("password"));

		InvalidProductQueryException exception =
				assertThrows(InvalidProductQueryException.class, () -> productService.list(pageable));

		assertThat(exception.getMessage()).contains("Cannot sort by 'password'");
		verify(productRepository, never()).findAll(any(Pageable.class));
	}

	private Sort capturedSort() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(productRepository).findAll(captor.capture());

		return captor.getValue().getSort();
	}

	private Product product(String name) {
		return new Product(name, "Home", 1_000, 5, 4.0);
	}
}
