package com.edstem.interviewprep.product.seed;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.edstem.interviewprep.product.entity.Product;
import com.edstem.interviewprep.product.repository.ProductRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductSeeder implements ApplicationRunner {

	static final int PRODUCT_COUNT = 100;

	private static final long RANDOM_SEED = 42L;
	private static final List<String> CATEGORIES =
			List.of("Electronics", "Books", "Clothing", "Home", "Toys", "Sports", "Beauty", "Grocery", "Garden", "Office");
	private static final List<String> ADJECTIVES =
			List.of("Compact", "Classic", "Premium", "Eco", "Smart", "Rugged", "Deluxe", "Basic", "Pro", "Mini");

	private final ProductRepository productRepository;

	public ProductSeeder(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (productRepository.count() > 0) {
			return;
		}

		productRepository.saveAll(generate());
	}

	List<Product> generate() {
		Random random = new Random(RANDOM_SEED);
		List<Product> products = new ArrayList<>(PRODUCT_COUNT);

		for (int i = 0; i < PRODUCT_COUNT; i++) {
			String category = CATEGORIES.get(i % CATEGORIES.size());
			String name = ADJECTIVES.get(random.nextInt(ADJECTIVES.size())) + " " + category + " Item " + (i + 1);
			long priceCents = 199 + random.nextInt(49_800);
			int stock = random.nextInt(5) == 0 ? 0 : random.nextInt(200);
			double rating = Math.round((1 + random.nextDouble() * 4) * 10) / 10.0;

			products.add(new Product(name, category, priceCents, stock, rating));
		}

		return products;
	}
}
