package com.edstem.interviewprep.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.edstem.interviewprep.common.dto.PageResponse;
import com.edstem.interviewprep.product.dto.request.ProductFilter;
import com.edstem.interviewprep.product.dto.request.ProductRequest;
import com.edstem.interviewprep.product.dto.response.ProductResponse;
import com.edstem.interviewprep.product.entity.Product;
import com.edstem.interviewprep.product.exception.InvalidProductQueryException;
import com.edstem.interviewprep.product.exception.ProductNotFoundException;
import com.edstem.interviewprep.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

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
		when(productRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(page);

		PageResponse<ProductResponse> response = productService.list(ProductFilter.none(), pageable);

		assertThat(response.content()).extracting(ProductResponse::name).containsExactly("Desk", "Lamp");
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(5);
		assertThat(response.totalPages()).isEqualTo(3);
	}

	@Test
	void listSortsByNameWhenNoSortRequested() {
		when(productRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(Page.empty());

		productService.list(ProductFilter.none(), PageRequest.of(0, 10));

		assertThat(capturedSort()).containsExactly(Sort.Order.asc("name"), Sort.Order.asc("id"));
	}

	@Test
	void listAppendsIdAsTieBreakerSoPagesAreStable() {
		when(productRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(Page.empty());

		productService.list(ProductFilter.none(), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "rating")));

		assertThat(capturedSort()).containsExactly(Sort.Order.desc("rating"), Sort.Order.asc("id"));
	}

	@Test
	void listRejectsUnknownSortField() {
		Pageable pageable = PageRequest.of(0, 10, Sort.by("password"));

		InvalidProductQueryException exception =
				assertThrows(InvalidProductQueryException.class,
						() -> productService.list(ProductFilter.none(), pageable));

		assertThat(exception.getMessage()).contains("Cannot sort by 'password'");
		verify(productRepository, never()).findAll(anySpecification(), any(Pageable.class));
	}

	@Test
	void listRejectsMinPriceAboveMaxPrice() {
		ProductFilter filter = new ProductFilter(null, 5_000L, 1_000L, false, null);

		InvalidProductQueryException exception =
				assertThrows(InvalidProductQueryException.class, () -> productService.list(filter, PageRequest.of(0, 10)));

		assertThat(exception.getMessage()).contains("minPriceCents");
		verify(productRepository, never()).findAll(anySpecification(), any(Pageable.class));
	}

	@Test
	void listAcceptsEqualMinAndMaxPrice() {
		when(productRepository.findAll(anySpecification(), any(Pageable.class))).thenReturn(Page.empty());
		ProductFilter filter = new ProductFilter(null, 1_000L, 1_000L, false, null);

		PageResponse<ProductResponse> response = productService.list(filter, PageRequest.of(0, 10));

		assertThat(response.content()).isEmpty();
	}

	@Test
	void getReturnsProduct() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.of(product("Desk")));

		ProductResponse response = productService.get(id);

		assertThat(response.name()).isEqualTo("Desk");
	}

	@Test
	void getThrowsWhenProductDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.empty());

		ProductNotFoundException exception = assertThrows(ProductNotFoundException.class, () -> productService.get(id));

		assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(exception.getMessage()).contains(id.toString());
	}

	@Test
	void createSavesProductFromRequest() {
		ProductRequest request = new ProductRequest("Lamp", "Home", 1_999L, 8, 4.1);
		when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

		ProductResponse response = productService.create(request);

		assertThat(response.name()).isEqualTo("Lamp");
		assertThat(response.category()).isEqualTo("Home");
		assertThat(response.priceCents()).isEqualTo(1_999L);
		assertThat(response.stock()).isEqualTo(8);
		assertThat(response.rating()).isEqualTo(4.1);
	}

	@Test
	void updateChangesEveryField() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.of(product("Old")));
		ProductRequest request = new ProductRequest("New", "Books", 700L, 0, 2.5);

		ProductResponse response = productService.update(id, request);

		assertThat(response.name()).isEqualTo("New");
		assertThat(response.category()).isEqualTo("Books");
		assertThat(response.priceCents()).isEqualTo(700L);
		assertThat(response.stock()).isZero();
		assertThat(response.rating()).isEqualTo(2.5);
	}

	@Test
	void updateThrowsWhenProductDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.empty());
		ProductRequest request = new ProductRequest("New", "Books", 700L, 0, 2.5);

		assertThrows(ProductNotFoundException.class, () -> productService.update(id, request));
	}

	@Test
	void deleteRemovesExistingProduct() {
		UUID id = UUID.randomUUID();
		Product product = product("Desk");
		when(productRepository.findById(id)).thenReturn(Optional.of(product));

		productService.delete(id);

		verify(productRepository).delete(product);
	}

	@Test
	void deleteThrowsWhenProductDoesNotExist() {
		UUID id = UUID.randomUUID();
		when(productRepository.findById(id)).thenReturn(Optional.empty());

		assertThrows(ProductNotFoundException.class, () -> productService.delete(id));

		verify(productRepository, never()).delete(any(Product.class));
	}

	private Specification<Product> anySpecification() {
		return ArgumentMatchers.<Specification<Product>>any();
	}

	private Sort capturedSort() {
		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(productRepository).findAll(anySpecification(), captor.capture());

		return captor.getValue().getSort();
	}

	private Product product(String name) {
		return new Product(name, "Home", 1_000, 5, 4.0);
	}
}
