package com.edstem.interviewprep.product.service;

import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.edstem.interviewprep.common.dto.PageResponse;
import com.edstem.interviewprep.product.dto.request.ProductFilter;
import com.edstem.interviewprep.product.dto.request.ProductRequest;
import com.edstem.interviewprep.product.dto.response.ProductResponse;
import com.edstem.interviewprep.product.entity.Product;
import com.edstem.interviewprep.product.exception.InvalidProductQueryException;
import com.edstem.interviewprep.product.exception.ProductNotFoundException;
import com.edstem.interviewprep.product.repository.ProductRepository;
import com.edstem.interviewprep.product.specification.ProductSpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

	public static final String PRODUCTS_CACHE = "products";

	private static final Set<String> SORTABLE_FIELDS =
			Set.of("id", "name", "category", "priceCents", "stock", "rating", "createdAt");
	private static final Sort DEFAULT_SORT = Sort.by("name");
	private static final Sort TIE_BREAKER = Sort.by("id");

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> list(ProductFilter filter, Pageable pageable) {
		validatePriceRange(filter);
		Pageable stablePageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sortFor(pageable));

		return PageResponse.of(
				productRepository.findAll(ProductSpecifications.from(filter), stablePageable),
				ProductResponse::from);
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = PRODUCTS_CACHE, key = "#id", sync = true)
	public ProductResponse get(UUID id) {
		return ProductResponse.from(findProduct(id));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		Product product = new Product(
				request.name(), request.category(), request.priceCents(), request.stock(), request.rating());

		return ProductResponse.from(productRepository.saveAndFlush(product));
	}

	@Transactional
	@CacheEvict(cacheNames = PRODUCTS_CACHE, key = "#id")
	public ProductResponse update(UUID id, ProductRequest request) {
		Product product = findProduct(id);
		product.update(request.name(), request.category(), request.priceCents(), request.stock(), request.rating());

		return ProductResponse.from(product);
	}

	@Transactional
	@CacheEvict(cacheNames = PRODUCTS_CACHE, key = "#id")
	public void delete(UUID id) {
		productRepository.delete(findProduct(id));
	}

	private Product findProduct(UUID id) {
		return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	private void validatePriceRange(ProductFilter filter) {
		Long min = filter.minPriceCents();
		Long max = filter.maxPriceCents();
		if (min != null && max != null && min > max) {
			throw new InvalidProductQueryException("minPriceCents must not be greater than maxPriceCents");
		}
	}

	private Sort sortFor(Pageable pageable) {
		Sort requested = pageable.getSort();
		requested.forEach(order -> {
			if (!SORTABLE_FIELDS.contains(order.getProperty())) {
				throw new InvalidProductQueryException(
						"Cannot sort by '" + order.getProperty() + "'. Sortable fields: " + new TreeSet<>(SORTABLE_FIELDS));
			}
		});

		Sort sort = requested.isSorted() ? requested : DEFAULT_SORT;

		return sort.and(TIE_BREAKER);
	}
}
