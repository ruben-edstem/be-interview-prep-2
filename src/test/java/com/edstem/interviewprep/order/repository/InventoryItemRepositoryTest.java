package com.edstem.interviewprep.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.edstem.interviewprep.order.entity.InventoryItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class InventoryItemRepositoryTest {

	@Autowired
	private InventoryItemRepository repository;

	@Test
	void reserveDecrementsStockWhenEnoughIsAvailable() {
		InventoryItem item = repository.saveAndFlush(new InventoryItem("Widget", 10));

		int updated = repository.reserve(item.getId(), 4);

		assertThat(updated).isEqualTo(1);
		assertThat(repository.findById(item.getId()).orElseThrow().getStock()).isEqualTo(6);
	}

	@Test
	void reserveCanTakeTheLastUnit() {
		InventoryItem item = repository.saveAndFlush(new InventoryItem("Widget", 3));

		int updated = repository.reserve(item.getId(), 3);

		assertThat(updated).isEqualTo(1);
		assertThat(repository.findById(item.getId()).orElseThrow().getStock()).isZero();
	}

	@Test
	void reserveChangesNothingWhenStockIsInsufficient() {
		InventoryItem item = repository.saveAndFlush(new InventoryItem("Widget", 2));

		int updated = repository.reserve(item.getId(), 3);

		assertThat(updated).isZero();
		assertThat(repository.findById(item.getId()).orElseThrow().getStock()).isEqualTo(2);
	}

	@Test
	void reserveReturnsZeroForUnknownProduct() {
		int updated = repository.reserve(UUID.randomUUID(), 1);

		assertThat(updated).isZero();
	}

	@Test
	void releaseIncrementsStock() {
		InventoryItem item = repository.saveAndFlush(new InventoryItem("Widget", 1));

		int updated = repository.release(item.getId(), 4);

		assertThat(updated).isEqualTo(1);
		assertThat(repository.findById(item.getId()).orElseThrow().getStock()).isEqualTo(5);
	}
}
