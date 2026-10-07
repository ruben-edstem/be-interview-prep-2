package com.edstem.interviewprep.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.edstem.interviewprep.product.dto.request.ProductRequest;
import com.edstem.interviewprep.product.dto.response.ProductResponse;
import com.edstem.interviewprep.product.entity.Product;
import com.edstem.interviewprep.product.exception.ProductNotFoundException;
import com.edstem.interviewprep.product.repository.ProductRepository;
import com.edstem.interviewprep.product.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ProductCatalogIntegrationTest {

	private static final String PRODUCTS = "/api/v1/products";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProductService productService;

	@MockitoSpyBean
	private ProductRepository productRepository;

	private final List<UUID> createdIds = new ArrayList<>();

	@AfterEach
	void removeProductsCreatedByTheTest() {
		productRepository.deleteAllById(createdIds);
		createdIds.clear();
	}

	@Test
	void startupSeedsOneHundredProducts() {
		assertThat(productRepository.count()).isEqualTo(100);
	}

	@Test
	void repeatedLookupsOfTheSameProductHitTheDatabaseOnlyOnce() {
		UUID id = createProduct("Cached Widget");
		clearInvocations(productRepository);

		ProductResponse first = productService.get(id);
		ProductResponse second = productService.get(id);
		ProductResponse third = productService.get(id);

		verify(productRepository, times(1)).findById(id);
		assertThat(second).isEqualTo(first);
		assertThat(third).isEqualTo(first);
	}

	@Test
	void lookupsOfDifferentProductsAreCachedIndependently() {
		UUID firstId = createProduct("First Widget");
		UUID secondId = createProduct("Second Widget");
		clearInvocations(productRepository);

		productService.get(firstId);
		productService.get(secondId);
		productService.get(firstId);
		productService.get(secondId);

		verify(productRepository, times(1)).findById(firstId);
		verify(productRepository, times(1)).findById(secondId);
	}

	@Test
	void lookupAfterUpdateReturnsNewDataFromTheDatabase() {
		UUID id = createProduct("Old Name");
		clearInvocations(productRepository);
		productService.get(id);
		productService.get(id);

		productService.update(id, new ProductRequest("New Name", "Home", 9_999L, 3, 4.2));
		clearInvocations(productRepository);
		ProductResponse afterUpdate = productService.get(id);
		productService.get(id);

		assertThat(afterUpdate.name()).isEqualTo("New Name");
		assertThat(afterUpdate.priceCents()).isEqualTo(9_999L);
		verify(productRepository, times(1)).findById(id);
	}

	@Test
	void lookupAfterDeleteIsNotFoundInsteadOfServedFromCache() {
		UUID id = createProduct("Doomed Widget");
		productService.get(id);
		productService.get(id);

		productService.delete(id);

		assertThatThrownBy(() -> productService.get(id)).isInstanceOf(ProductNotFoundException.class);
	}

	@Test
	void failedLookupIsNotCached() {
		UUID unknownId = UUID.randomUUID();

		assertThatThrownBy(() -> productService.get(unknownId)).isInstanceOf(ProductNotFoundException.class);
		assertThatThrownBy(() -> productService.get(unknownId)).isInstanceOf(ProductNotFoundException.class);

		verify(productRepository, times(2)).findById(unknownId);
	}

	@Test
	void failedUpdateOfAnotherProductKeepsTheCachedEntry() {
		UUID id = createProduct("Stable Widget");
		clearInvocations(productRepository);
		productService.get(id);
		ProductRequest request = new ProductRequest("Ghost", "Home", 100L, 1, 3.0);

		assertThatThrownBy(() -> productService.update(UUID.randomUUID(), request))
				.isInstanceOf(ProductNotFoundException.class);
		productService.get(id);

		verify(productRepository, times(1)).findById(id);
	}

	@Test
	void concurrentReadersNeverSeeStaleDataOnceAnUpdateHasReturned() throws Exception {
		UUID id = createProduct("Version 0");
		ExecutorService executor = Executors.newFixedThreadPool(3);
		AtomicBoolean updating = new AtomicBoolean(true);
		List<Future<?>> readers = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			readers.add(executor.submit(() -> {
				while (updating.get()) {
					productService.get(id);
				}
			}));
		}

		try {
			for (int version = 1; version <= 100; version++) {
				String name = "Version " + version;
				productService.update(id, new ProductRequest(name, "Home", 1_000L + version, 5, 4.0));

				assertThat(productService.get(id).name()).isEqualTo(name);
			}
		} finally {
			updating.set(false);
			for (Future<?> reader : readers) {
				reader.get();
			}
			executor.shutdown();
		}
	}

	@Test
	void listingSupportsEveryFilterAtOnceThroughTheApi() throws Exception {
		UUID id = createProduct("Zebra Sprocket", "Gadgets", 7_500, 4, 4.9);

		mockMvc.perform(get(PRODUCTS)
						.param("category", "Gadgets")
						.param("minPriceCents", "7000")
						.param("maxPriceCents", "8000")
						.param("inStock", "true")
						.param("search", "sprocket")
						.param("sort", "rating,desc")
						.param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1))
				.andExpect(jsonPath("$.content[0].id").value(id.toString()));
	}

	@Test
	void listingReportsTotalCountAndPagesForTheSeededCatalog() throws Exception {
		mockMvc.perform(get(PRODUCTS).param("size", "30").param("sort", "priceCents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(30))
				.andExpect(jsonPath("$.totalElements").value(100))
				.andExpect(jsonPath("$.totalPages").value(4));
	}

	@Test
	void listingCapsOversizedPagesAtOneHundred() throws Exception {
		mockMvc.perform(get(PRODUCTS).param("size", "1000"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(100))
				.andExpect(jsonPath("$.content.length()").value(100));
	}

	@Test
	void listingRejectsUnknownSortFieldWithBadRequest() throws Exception {
		mockMvc.perform(get(PRODUCTS).param("sort", "nope,asc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PRODUCT_QUERY"));
	}

	@Test
	void listingRejectsInvertedPriceRange() throws Exception {
		mockMvc.perform(get(PRODUCTS).param("minPriceCents", "900").param("maxPriceCents", "100"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_PRODUCT_QUERY"));
	}

	private UUID createProduct(String name) {
		return createProduct(name, "Home", 1_000, 5, 4.0);
	}

	private UUID createProduct(String name, String category, long priceCents, int stock, double rating) {
		UUID id = productRepository.saveAndFlush(new Product(name, category, priceCents, stock, rating)).getId();
		createdIds.add(id);

		return id;
	}
}
