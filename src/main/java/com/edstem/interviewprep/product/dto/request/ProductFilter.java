package com.edstem.interviewprep.product.dto.request;

public record ProductFilter(
		String category,
		Long minPriceCents,
		Long maxPriceCents,
		boolean inStockOnly,
		String search) {

	public static ProductFilter none() {
		return new ProductFilter(null, null, null, false, null);
	}
}
