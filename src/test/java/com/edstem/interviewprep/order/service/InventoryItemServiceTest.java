package com.edstem.interviewprep.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import com.edstem.interviewprep.order.entity.InventoryItem;
import com.edstem.interviewprep.order.exception.InventoryItemNotFoundException;
import com.edstem.interviewprep.order.repository.InventoryItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceTest {

	@Mock
	private InventoryItemRepository repository;

	@InjectMocks
	private InventoryItemService service;

	@Test
	void createStoresTheItemWithItsStock() {
		when(repository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

		InventoryItem created = service.create("Widget", 7);

		assertThat(created.getName()).isEqualTo("Widget");
		assertThat(created.getStock()).isEqualTo(7);
	}

	@Test
	void getReturnsTheItem() {
		UUID id = UUID.randomUUID();
		InventoryItem item = new InventoryItem("Widget", 7);
		when(repository.findById(id)).thenReturn(Optional.of(item));

		assertThat(service.get(id)).isSameAs(item);
	}

	@Test
	void getFailsForAnUnknownItem() {
		UUID id = UUID.randomUUID();
		when(repository.findById(id)).thenReturn(Optional.empty());

		assertThrows(InventoryItemNotFoundException.class, () -> service.get(id));
	}
}
