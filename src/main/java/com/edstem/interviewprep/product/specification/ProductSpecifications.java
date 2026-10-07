package com.edstem.interviewprep.product.specification;

import java.util.Locale;

import com.edstem.interviewprep.product.dto.request.ProductFilter;
import com.edstem.interviewprep.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

	private static final char LIKE_ESCAPE = '\\';

	private ProductSpecifications() {
	}

	public static Specification<Product> from(ProductFilter filter) {
		return Specification.allOf(
				hasCategory(filter.category()),
				priceAtLeast(filter.minPriceCents()),
				priceAtMost(filter.maxPriceCents()),
				inStock(filter.inStockOnly()),
				nameContains(filter.search()));
	}

	private static Specification<Product> hasCategory(String category) {
		if (!StringUtils.hasText(category)) {
			return null;
		}

		return (root, query, builder) -> builder.equal(root.get("category"), category.trim());
	}

	private static Specification<Product> priceAtLeast(Long minPriceCents) {
		if (minPriceCents == null) {
			return null;
		}

		return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("priceCents"), minPriceCents);
	}

	private static Specification<Product> priceAtMost(Long maxPriceCents) {
		if (maxPriceCents == null) {
			return null;
		}

		return (root, query, builder) -> builder.lessThanOrEqualTo(root.get("priceCents"), maxPriceCents);
	}

	private static Specification<Product> inStock(boolean inStockOnly) {
		if (!inStockOnly) {
			return null;
		}

		return (root, query, builder) -> builder.greaterThan(root.get("stock"), 0);
	}

	private static Specification<Product> nameContains(String search) {
		if (!StringUtils.hasText(search)) {
			return null;
		}

		String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";

		return (root, query, builder) ->
				builder.like(builder.lower(root.get("name")), pattern, LIKE_ESCAPE);
	}

	private static String escapeLike(String value) {
		return value
				.replace("\\", "\\\\")
				.replace("%", "\\%")
				.replace("_", "\\_");
	}
}
