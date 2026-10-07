package com.edstem.interviewprep.product.controller;

import com.edstem.interviewprep.common.dto.PageResponse;
import com.edstem.interviewprep.product.dto.request.ProductFilter;
import com.edstem.interviewprep.product.dto.response.ProductResponse;
import com.edstem.interviewprep.product.service.ProductService;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public PageResponse<ProductResponse> list(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) Long minPriceCents,
			@RequestParam(required = false) Long maxPriceCents,
			@RequestParam(defaultValue = "false") boolean inStock,
			@RequestParam(required = false) String search,
			Pageable pageable) {
		ProductFilter filter = new ProductFilter(category, minPriceCents, maxPriceCents, inStock, search);

		return productService.list(filter, pageable);
	}
}
