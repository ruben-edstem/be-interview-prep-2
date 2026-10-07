package com.edstem.interviewprep.product.controller;

import java.net.URI;
import java.util.UUID;

import com.edstem.interviewprep.common.dto.PageResponse;
import com.edstem.interviewprep.product.dto.request.ProductFilter;
import com.edstem.interviewprep.product.dto.request.ProductRequest;
import com.edstem.interviewprep.product.dto.response.ProductResponse;
import com.edstem.interviewprep.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

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

	@GetMapping("/{id}")
	public ProductResponse get(@PathVariable UUID id) {
		return productService.get(id);
	}

	@PostMapping
	public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
		ProductResponse created = productService.create(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(created.id())
				.toUri();

		return ResponseEntity.created(location).body(created);
	}

	@PutMapping("/{id}")
	public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable UUID id) {
		productService.delete(id);

		return ResponseEntity.noContent().build();
	}
}
