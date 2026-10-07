package com.edstem.interviewprep.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.UUID;

import com.edstem.interviewprep.order.entity.CustomerOrder;
import com.edstem.interviewprep.order.entity.OrderItem;
import com.edstem.interviewprep.order.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class OrderRepositoryTest {

	@Autowired
	private OrderRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void savesAnOrderWithItsItemsAsPlaced() {
		UUID productId = UUID.randomUUID();
		CustomerOrder saved = repository.saveAndFlush(order("key-1", new OrderItem(productId, 2)));
		entityManager.clear();

		CustomerOrder found = repository.findWithItemsById(saved.getId()).orElseThrow();

		assertThat(found.getStatus()).isEqualTo(OrderStatus.PLACED);
		assertThat(found.getCreatedAt()).isNotNull();
		assertThat(found.getItems()).extracting(OrderItem::getProductId).containsExactly(productId);
		assertThat(found.getItems()).extracting(OrderItem::getQuantity).containsExactly(2L);
	}

	@Test
	void findsAnOrderByItsIdempotencyKey() {
		CustomerOrder saved = repository.saveAndFlush(order("key-2", new OrderItem(UUID.randomUUID(), 1)));
		entityManager.clear();

		assertThat(repository.findByIdempotencyKey("key-2").orElseThrow().getId()).isEqualTo(saved.getId());
		assertThat(repository.findByIdempotencyKey("other")).isEmpty();
	}

	@Test
	void rejectsADuplicateIdempotencyKey() {
		repository.saveAndFlush(order("dup", new OrderItem(UUID.randomUUID(), 1)));

		CustomerOrder duplicate = order("dup", new OrderItem(UUID.randomUUID(), 1));

		assertThrows(DataIntegrityViolationException.class, () -> repository.saveAndFlush(duplicate));
	}

	@Test
	void markCancelledOnlyTransitionsAPlacedOrderOnce() {
		CustomerOrder saved = repository.saveAndFlush(order("key-3", new OrderItem(UUID.randomUUID(), 1)));

		int first = repository.markCancelled(saved.getId());
		int second = repository.markCancelled(saved.getId());

		assertThat(first).isEqualTo(1);
		assertThat(second).isZero();
		assertThat(repository.findWithItemsById(saved.getId()).orElseThrow().getStatus())
				.isEqualTo(OrderStatus.CANCELLED);
	}

	private CustomerOrder order(String key, OrderItem... items) {
		return new CustomerOrder(key, "hash", List.of(items));
	}
}
