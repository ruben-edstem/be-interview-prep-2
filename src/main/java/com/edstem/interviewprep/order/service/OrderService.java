package com.edstem.interviewprep.order.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import com.edstem.interviewprep.order.entity.CustomerOrder;
import com.edstem.interviewprep.order.entity.OrderItem;
import com.edstem.interviewprep.order.exception.IdempotencyKeyReusedException;
import com.edstem.interviewprep.order.exception.InsufficientStockException;
import com.edstem.interviewprep.order.exception.InvalidIdempotencyKeyException;
import com.edstem.interviewprep.order.exception.InventoryItemNotFoundException;
import com.edstem.interviewprep.order.exception.OrderNotFoundException;
import com.edstem.interviewprep.order.repository.InventoryItemRepository;
import com.edstem.interviewprep.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderService {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	private static final int MAX_KEY_LENGTH = 100;

	private final OrderRepository orders;
	private final InventoryItemRepository inventory;
	private final TransactionTemplate transaction;

	public OrderService(OrderRepository orders, InventoryItemRepository inventory, TransactionTemplate transaction) {
		this.orders = orders;
		this.inventory = inventory;
		this.transaction = transaction;
	}

	public PlacedOrder place(String idempotencyKey, List<OrderLine> lines) {
		if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > MAX_KEY_LENGTH) {
			throw new InvalidIdempotencyKeyException(MAX_KEY_LENGTH);
		}
		Map<UUID, Long> quantities = mergeByProduct(lines);
		String requestHash = fingerprint(quantities);

		Optional<CustomerOrder> existing = orders.findByIdempotencyKey(idempotencyKey);
		if (existing.isPresent()) {
			return replay(existing.get(), requestHash);
		}

		try {
			CustomerOrder created = transaction.execute(status -> reserveAndSave(idempotencyKey, requestHash, quantities));
			return new PlacedOrder(created, false);
		} catch (DataIntegrityViolationException e) {
			log.info("Concurrent retry detected for idempotency key {}", idempotencyKey);
			CustomerOrder winner = orders.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
			return replay(winner, requestHash);
		}
	}

	@Transactional
	public CustomerOrder cancel(UUID id) {
		if (orders.markCancelled(id) == 0) {
			return find(id);
		}

		CustomerOrder order = find(id);
		order.getItems().stream()
				.sorted((a, b) -> a.getProductId().compareTo(b.getProductId()))
				.forEach(item -> inventory.release(item.getProductId(), item.getQuantity()));
		return order;
	}

	@Transactional(readOnly = true)
	public CustomerOrder get(UUID id) {
		return find(id);
	}

	private CustomerOrder reserveAndSave(String idempotencyKey, String requestHash, Map<UUID, Long> quantities) {
		List<OrderItem> items = quantities.entrySet().stream()
				.map(entry -> new OrderItem(entry.getKey(), entry.getValue()))
				.toList();
		CustomerOrder order = orders.saveAndFlush(new CustomerOrder(idempotencyKey, requestHash, items));

		quantities.forEach(this::reserve);
		return order;
	}

	private void reserve(UUID productId, long quantity) {
		if (inventory.reserve(productId, quantity) == 1) {
			return;
		}
		if (!inventory.existsById(productId)) {
			throw new InventoryItemNotFoundException(productId);
		}
		throw new InsufficientStockException(productId, quantity);
	}

	private PlacedOrder replay(CustomerOrder existing, String requestHash) {
		if (!existing.getRequestHash().equals(requestHash)) {
			throw new IdempotencyKeyReusedException(existing.getIdempotencyKey());
		}
		return new PlacedOrder(existing, true);
	}

	private CustomerOrder find(UUID id) {
		return orders.findWithItemsById(id).orElseThrow(() -> new OrderNotFoundException(id));
	}

	static Map<UUID, Long> mergeByProduct(List<OrderLine> lines) {
		Map<UUID, Long> quantities = new TreeMap<>();
		lines.forEach(line -> quantities.merge(line.productId(), line.quantity(), Long::sum));
		return quantities;
	}

	static String fingerprint(Map<UUID, Long> quantities) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256")
					.digest(quantities.toString().getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is unavailable", e);
		}
	}
}
