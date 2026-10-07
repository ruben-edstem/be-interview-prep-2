package com.edstem.interviewprep.product.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.edstem.interviewprep.product.entity.Product;
import com.edstem.interviewprep.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductSeederTest {

	@Mock
	private ProductRepository productRepository;

	@InjectMocks
	private ProductSeeder productSeeder;

	@Captor
	private ArgumentCaptor<List<Product>> savedProducts;

	@Test
	void seedsOneHundredProductsWhenCatalogIsEmpty() {
		when(productRepository.count()).thenReturn(0L);

		productSeeder.run(null);

		verify(productRepository).saveAll(savedProducts.capture());
		assertThat(savedProducts.getValue()).hasSize(100);
	}

	@Test
	void seededProductsHaveValidValuesAcrossSeveralCategories() {
		List<Product> products = productSeeder.generate();

		assertThat(products).extracting(Product::getCategory).doesNotContainNull().hasSizeGreaterThan(1);
		assertThat(products).extracting(Product::getCategory).containsAnyOf("Electronics", "Books");
		assertThat(products).allSatisfy(product -> {
			assertThat(product.getName()).isNotBlank();
			assertThat(product.getPriceCents()).isPositive();
			assertThat(product.getStock()).isNotNegative();
			assertThat(product.getRating()).isBetween(1.0, 5.0);
		});
		assertThat(products).anyMatch(product -> product.getStock() == 0);
		assertThat(products).anyMatch(product -> product.getStock() > 0);
	}

	@Test
	void doesNotSeedWhenProductsAlreadyExist() {
		when(productRepository.count()).thenReturn(7L);

		productSeeder.run(null);

		verify(productRepository, never()).saveAll(anyList());
	}
}
