package com.edstem.interviewprep.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.edstem.interviewprep.order.entity.CustomerOrder;
import com.edstem.interviewprep.order.entity.OrderItem;
import com.edstem.interviewprep.order.entity.OrderStatus;
import com.edstem.interviewprep.order.exception.IdempotencyKeyReusedException;
import com.edstem.interviewprep.order.exception.InsufficientStockException;
import com.edstem.interviewprep.order.exception.InvalidIdempotencyKeyException;
import com.edstem.interviewprep.order.exception.InvalidQuantityException;
import com.edstem.interviewprep.order.exception.InventoryItemNotFoundException;
import com.edstem.interviewprep.order.exception.OrderNotFoundException;
import com.edstem.interviewprep.order.repository.InventoryItemRepository;
import com.edstem.interviewprep.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	private static final UUID LOW_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID HIGH_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

	@Mock
	private OrderRepository orders;

	@Mock
	private InventoryItemRepository inventory;

	@Mock
	private TransactionTemplate transaction;

	private OrderService service;

	@BeforeEach
	void setUp() {
		service = new OrderService(orders, inventory, transaction);
	}

	@Test
	void placeReservesEveryItemAndSavesTheOrder() {
		runTransactionsInline();
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.empty());
		when(orders.saveAndFlush(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(inventory.reserve(any(UUID.class), anyLong())).thenReturn(1);

		PlacedOrder placed = service.place("key", List.of(new OrderLine(HIGH_ID, 2), new OrderLine(LOW_ID, 1)));

		assertThat(placed.replayed()).isFalse();
		assertThat(placed.order().getStatus()).isEqualTo(OrderStatus.PLACED);
		assertThat(placed.order().getItems()).extracting(OrderItem::getProductId).containsExactly(LOW_ID, HIGH_ID);
		InOrder reservations = inOrder(inventory);
		reservations.verify(inventory).reserve(LOW_ID, 1);
		reservations.verify(inventory).reserve(HIGH_ID, 2);
	}

	@Test
	void placeMergesRepeatedProductsIntoOneReservation() {
		runTransactionsInline();
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.empty());
		when(orders.saveAndFlush(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(inventory.reserve(LOW_ID, 5)).thenReturn(1);

		PlacedOrder placed = service.place("key", List.of(new OrderLine(LOW_ID, 2), new OrderLine(LOW_ID, 3)));

		assertThat(placed.order().getItems()).hasSize(1);
		assertThat(placed.order().getItems().get(0).getQuantity()).isEqualTo(5);
		verify(inventory).reserve(LOW_ID, 5);
	}

	@Test
	void placeRejectsRepeatedLinesWhoseTotalOverflows() {
		List<OrderLine> lines = List.of(new OrderLine(LOW_ID, Long.MAX_VALUE), new OrderLine(LOW_ID, 1));

		assertThrows(InvalidQuantityException.class, () -> service.place("key", lines));

		verify(orders, never()).saveAndFlush(any(CustomerOrder.class));
		verify(inventory, never()).reserve(any(UUID.class), anyLong());
	}

	@Test
	void placeRejectsZeroAndNegativeQuantities() {
		assertThrows(InvalidQuantityException.class,
				() -> service.place("key", List.of(new OrderLine(LOW_ID, 0))));
		assertThrows(InvalidQuantityException.class,
				() -> service.place("key", List.of(new OrderLine(LOW_ID, -5))));

		verify(inventory, never()).reserve(any(UUID.class), anyLong());
	}

	@Test
	void placeFailsWithConflictWhenStockIsInsufficient() {
		runTransactionsInline();
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.empty());
		when(orders.saveAndFlush(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(inventory.reserve(LOW_ID, 5)).thenReturn(0);
		when(inventory.existsById(LOW_ID)).thenReturn(true);

		InsufficientStockException thrown = assertThrows(InsufficientStockException.class,
				() -> service.place("key", List.of(new OrderLine(LOW_ID, 5))));

		assertThat(thrown.getMessage()).contains(LOW_ID.toString());
	}

	@Test
	void placeFailsWithNotFoundForAnUnknownProduct() {
		runTransactionsInline();
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.empty());
		when(orders.saveAndFlush(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(inventory.reserve(LOW_ID, 1)).thenReturn(0);
		when(inventory.existsById(LOW_ID)).thenReturn(false);

		assertThrows(InventoryItemNotFoundException.class,
				() -> service.place("key", List.of(new OrderLine(LOW_ID, 1))));
	}

	@Test
	void placeReturnsTheExistingOrderForARetriedRequest() {
		CustomerOrder existing = placedOrder("key", List.of(new OrderLine(LOW_ID, 1)));
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.of(existing));

		PlacedOrder placed = service.place("key", List.of(new OrderLine(LOW_ID, 1)));

		assertThat(placed.replayed()).isTrue();
		assertThat(placed.order()).isSameAs(existing);
		verify(inventory, never()).reserve(any(UUID.class), anyLong());
	}

	@Test
	void placeTreatsReorderedLinesAsTheSameRequest() {
		CustomerOrder existing = placedOrder("key", List.of(new OrderLine(LOW_ID, 1), new OrderLine(HIGH_ID, 2)));
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.of(existing));

		PlacedOrder placed = service.place("key", List.of(new OrderLine(HIGH_ID, 2), new OrderLine(LOW_ID, 1)));

		assertThat(placed.replayed()).isTrue();
	}

	@Test
	void placeRejectsAKeyReusedWithADifferentRequest() {
		CustomerOrder existing = placedOrder("key", List.of(new OrderLine(LOW_ID, 1)));
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.of(existing));

		assertThrows(IdempotencyKeyReusedException.class,
				() -> service.place("key", List.of(new OrderLine(LOW_ID, 2))));
	}

	@Test
	void placeReturnsTheWinnerWhenAConcurrentRetryWonTheRace() {
		CustomerOrder winner = placedOrder("key", List.of(new OrderLine(LOW_ID, 1)));
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.empty(), Optional.of(winner));
		when(transaction.execute(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

		PlacedOrder placed = service.place("key", List.of(new OrderLine(LOW_ID, 1)));

		assertThat(placed.replayed()).isTrue();
		assertThat(placed.order()).isSameAs(winner);
	}

	@Test
	void placeRethrowsAnIntegrityViolationThatIsNotADuplicateKey() {
		when(orders.findByIdempotencyKey("key")).thenReturn(Optional.empty(), Optional.empty());
		when(transaction.execute(any())).thenThrow(new DataIntegrityViolationException("other"));

		assertThrows(DataIntegrityViolationException.class,
				() -> service.place("key", List.of(new OrderLine(LOW_ID, 1))));
	}

	@Test
	void placeRejectsBlankAndOversizedKeys() {
		List<OrderLine> lines = List.of(new OrderLine(LOW_ID, 1));

		assertThrows(InvalidIdempotencyKeyException.class, () -> service.place(" ", lines));
		assertThrows(InvalidIdempotencyKeyException.class, () -> service.place("k".repeat(101), lines));
		verify(orders, never()).findByIdempotencyKey(any());
	}

	@Test
	void cancelReleasesStockForEveryItem() {
		UUID orderId = UUID.randomUUID();
		CustomerOrder order = placedOrder("key", List.of(new OrderLine(HIGH_ID, 2), new OrderLine(LOW_ID, 1)));
		when(orders.markCancelled(orderId)).thenReturn(1);
		when(orders.findWithItemsById(orderId)).thenReturn(Optional.of(order));

		service.cancel(orderId);

		verify(inventory).release(LOW_ID, 1);
		verify(inventory).release(HIGH_ID, 2);
	}

	@Test
	void cancelIsANoOpForAnAlreadyCancelledOrder() {
		UUID orderId = UUID.randomUUID();
		CustomerOrder order = placedOrder("key", List.of(new OrderLine(LOW_ID, 1)));
		when(orders.markCancelled(orderId)).thenReturn(0);
		when(orders.findWithItemsById(orderId)).thenReturn(Optional.of(order));

		CustomerOrder result = service.cancel(orderId);

		assertThat(result).isSameAs(order);
		verify(inventory, never()).release(any(UUID.class), eq(1L));
	}

	@Test
	void cancelFailsForAnUnknownOrder() {
		UUID orderId = UUID.randomUUID();
		when(orders.markCancelled(orderId)).thenReturn(0);
		when(orders.findWithItemsById(orderId)).thenReturn(Optional.empty());

		assertThrows(OrderNotFoundException.class, () -> service.cancel(orderId));
	}

	@Test
	void getFailsForAnUnknownOrder() {
		UUID orderId = UUID.randomUUID();
		when(orders.findWithItemsById(orderId)).thenReturn(Optional.empty());

		assertThrows(OrderNotFoundException.class, () -> service.get(orderId));
	}

	@SuppressWarnings("unchecked")
	private void runTransactionsInline() {
		when(transaction.execute(any())).thenAnswer(invocation ->
				((TransactionCallback<Object>) invocation.getArgument(0)).doInTransaction(null));
	}

	private CustomerOrder placedOrder(String key, List<OrderLine> lines) {
		List<OrderItem> items = lines.stream().map(line -> new OrderItem(line.productId(), line.quantity())).toList();
		String requestHash = OrderService.fingerprint(OrderService.mergeByProduct(lines));
		return new CustomerOrder(key, requestHash, items);
	}
}
