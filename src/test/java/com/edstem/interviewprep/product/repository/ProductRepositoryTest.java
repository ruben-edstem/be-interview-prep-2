package com.edstem.interviewprep.product.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.List;

import com.edstem.interviewprep.product.dto.request.ProductFilter;
import com.edstem.interviewprep.product.entity.Product;
import com.edstem.interviewprep.product.specification.ProductSpecifications;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@DataJpaTest
class ProductRepositoryTest {

	@Autowired
	private ProductRepository productRepository;

	@BeforeEach
	void seedCatalog() {
		productRepository.save(new Product("Java Handbook", "Books", 2_500, 10, 4.5));
		productRepository.save(new Product("Spring Cookbook", "Books", 4_000, 0, 4.0));
		productRepository.save(new Product("Desk Lamp", "Home", 1_500, 7, 3.5));
		productRepository.save(new Product("Standing Desk", "Home", 30_000, 2, 4.8));
		productRepository.save(new Product("Discount 100% Mug", "Home", 500, 50, 2.0));
		productRepository.saveAndFlush(new Product("Snake_case Poster", "Home", 900, 5, 3.0));
	}

	@Test
	void savePopulatesIdAndCreatedAt() {
		Product saved = productRepository.saveAndFlush(new Product("Notebook", "Office", 300, 1, 4.0));

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getCreatedAt()).isNotNull();
	}

	@Test
	void noFilterReturnsEveryProduct() {
		Page<Product> page = find(ProductFilter.none());

		assertThat(page.getTotalElements()).isEqualTo(6);
	}

	@Test
	void filtersByCategory() {
		Page<Product> page = find(new ProductFilter("Books", null, null, false, null));

		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Java Handbook", "Spring Cookbook");
	}

	@Test
	void filtersByPriceRangeInclusively() {
		Page<Product> page = find(new ProductFilter(null, 1_500L, 2_500L, false, null));

		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Desk Lamp", "Java Handbook");
	}

	@Test
	void filtersByMinimumPriceAlone() {
		Page<Product> page = find(new ProductFilter(null, 10_000L, null, false, null));

		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Standing Desk");
	}

	@Test
	void inStockOnlyExcludesProductsWithZeroStock() {
		Page<Product> page = find(new ProductFilter("Books", null, null, true, null));

		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Java Handbook");
	}

	@Test
	void searchMatchesNamePartIgnoringCase() {
		Page<Product> page = find(new ProductFilter(null, null, null, false, "DESK"));

		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Desk Lamp", "Standing Desk");
	}

	@Test
	void searchTreatsPercentAndUnderscoreAsLiteralText() {
		Page<Product> percent = find(new ProductFilter(null, null, null, false, "100%"));
		Page<Product> underscore = find(new ProductFilter(null, null, null, false, "snake_c"));
		Page<Product> wildcardAttempt = find(new ProductFilter(null, null, null, false, "%"));

		assertThat(percent.getContent()).extracting(Product::getName).containsExactly("Discount 100% Mug");
		assertThat(underscore.getContent()).extracting(Product::getName).containsExactly("Snake_case Poster");
		assertThat(wildcardAttempt.getContent()).extracting(Product::getName).containsExactly("Discount 100% Mug");
	}

	@Test
	void allFiltersCombineInOneQuery() {
		ProductFilter filter = new ProductFilter("Home", 1_000L, 40_000L, true, "desk");

		Page<Product> page = find(filter);

		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Desk Lamp", "Standing Desk");
		assertThat(page.getTotalElements()).isEqualTo(2);
	}

	@Test
	void combinedFiltersWithNoMatchReturnEmptyPage() {
		Page<Product> page = find(new ProductFilter("Books", 10_000L, null, true, null));

		assertThat(page.getContent()).isEmpty();
		assertThat(page.getTotalPages()).isZero();
	}

	@Test
	void totalCountAndPagesReflectFilterNotPageSize() {
		Page<Product> page = productRepository.findAll(
				ProductSpecifications.from(new ProductFilter("Home", null, null, false, null)),
				PageRequest.of(1, 2, Sort.by("name")));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(page.getContent()).hasSize(2);
	}

	@Test
	void sortsByAnyFieldDescending() {
		List<Product> products = productRepository
				.findAll(ProductSpecifications.from(ProductFilter.none()), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "priceCents")))
				.getContent();

		assertThat(products).extracting(Product::getPriceCents).isSortedAccordingTo(Comparator.reverseOrder());
	}

	private Page<Product> find(ProductFilter filter) {
		return productRepository.findAll(ProductSpecifications.from(filter), PageRequest.of(0, 20, Sort.by("name")));
	}
}
