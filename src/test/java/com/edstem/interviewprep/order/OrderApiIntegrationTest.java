package com.edstem.interviewprep.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.edstem.interviewprep.order.repository.InventoryItemRepository;
import com.edstem.interviewprep.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiIntegrationTest {

	private static final int CONCURRENT_ORDERS = 50;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private InventoryItemRepository inventory;

	@Autowired
	private OrderRepository orders;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void fiftySimultaneousOrdersForStockOfTenSucceedExactlyTenTimes() throws Exception {
		String productId = createProduct("Contended widget", 10);
		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ORDERS);
		CountDownLatch startGate = new CountDownLatch(1);
		List<Callable<Integer>> requests = new ArrayList<>();
		for (int i = 0; i < CONCURRENT_ORDERS; i++) {
			String key = "race-" + UUID.randomUUID();
			requests.add(() -> {
				startGate.await();
				return placeOrder(key, line(productId, 1)).andReturn().getResponse().getStatus();
			});
		}

		List<Future<Integer>> futures = new ArrayList<>();
		for (Callable<Integer> request : requests) {
			futures.add(executor.submit(request));
		}
		startGate.countDown();
		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> future : futures) {
			statuses.add(future.get());
		}
		executor.shutdown();

		assertThat(statuses.stream().filter(code -> code == 201)).hasSize(10);
		assertThat(statuses.stream().filter(code -> code == 409)).hasSize(CONCURRENT_ORDERS - 10);
		assertThat(stockOf(productId)).isZero();
		assertThat(ordersContaining(productId)).isEqualTo(10);
	}

	@Test
	void retryingTheSameRequestCreatesOnlyOneOrderAndReservesStockOnce() throws Exception {
		String productId = createProduct("Retried widget", 5);
		String key = "retry-" + UUID.randomUUID();

		JsonNode first = json(placeOrder(key, line(productId, 2)).andExpect(status().isCreated()));
		JsonNode retry = json(placeOrder(key, line(productId, 2))
				.andExpect(status().isOk())
				.andExpect(header().string("Idempotent-Replayed", "true")));

		assertThat(retry.get("id").asText()).isEqualTo(first.get("id").asText());
		assertThat(stockOf(productId)).isEqualTo(3);
		assertThat(ordersContaining(productId)).isEqualTo(1);
	}

	@Test
	void simultaneousRetriesOfTheSameRequestCreateOneOrder() throws Exception {
		String productId = createProduct("Double-clicked widget", 5);
		String key = "double-" + UUID.randomUUID();
		int attempts = 10;
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		CountDownLatch startGate = new CountDownLatch(1);
		List<Future<String>> futures = new ArrayList<>();
		for (int i = 0; i < attempts; i++) {
			futures.add(executor.submit(() -> {
				startGate.await();
				return placeOrder(key, line(productId, 1)).andExpect(status().is2xxSuccessful())
						.andReturn().getResponse().getContentAsString();
			}));
		}

		startGate.countDown();
		List<String> orderIds = new ArrayList<>();
		for (Future<String> future : futures) {
			orderIds.add(objectMapper.readTree(future.get()).get("id").asText());
		}
		executor.shutdown();

		assertThat(orderIds).hasSize(attempts).containsOnly(orderIds.get(0));
		assertThat(stockOf(productId)).isEqualTo(4);
		assertThat(ordersContaining(productId)).isEqualTo(1);
	}

	@Test
	void reusingAKeyForADifferentRequestIsRejected() throws Exception {
		String productId = createProduct("Keyed widget", 5);
		String key = "reuse-" + UUID.randomUUID();
		placeOrder(key, line(productId, 1)).andExpect(status().isCreated());

		placeOrder(key, line(productId, 2))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.error").value("IDEMPOTENCY_KEY_REUSED"));

		assertThat(stockOf(productId)).isEqualTo(4);
	}

	@Test
	void orderIsAllOrNothingWhenOneItemLacksStock() throws Exception {
		String plentiful = createProduct("Plentiful widget", 10);
		String scarce = createProduct("Scarce widget", 1);

		placeOrder("partial-" + UUID.randomUUID(), line(plentiful, 3), line(scarce, 2))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("INSUFFICIENT_STOCK"))
				.andExpect(jsonPath("$.message").value(containsString(scarce)));

		assertThat(stockOf(plentiful)).isEqualTo(10);
		assertThat(stockOf(scarce)).isEqualTo(1);
		assertThat(ordersContaining(plentiful)).isZero();
	}

	@Test
	void failedOrderCanBeRetriedWithTheSameKeyOnceStockReturns() throws Exception {
		String productId = createProduct("Restocked widget", 1);
		String key = "failed-" + UUID.randomUUID();
		placeOrder(key, line(productId, 2)).andExpect(status().isConflict());

		placeOrder(key, line(productId, 1)).andExpect(status().isCreated());

		assertThat(stockOf(productId)).isZero();
	}

	@Test
	void cancellingAnOrderReturnsItsStockExactlyOnce() throws Exception {
		String productId = createProduct("Cancelled widget", 5);
		String orderId = json(placeOrder("cancel-" + UUID.randomUUID(), line(productId, 3))
				.andExpect(status().isCreated())).get("id").asText();
		assertThat(stockOf(productId)).isEqualTo(2);

		mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
		mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		assertThat(stockOf(productId)).isEqualTo(5);
		mockMvc.perform(get("/api/v1/orders/" + orderId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}

	@Test
	void simultaneousCancellationsReturnStockOnlyOnce() throws Exception {
		String productId = createProduct("Double-cancelled widget", 5);
		String orderId = json(placeOrder("dcancel-" + UUID.randomUUID(), line(productId, 4))
				.andExpect(status().isCreated())).get("id").asText();
		int attempts = 10;
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		CountDownLatch startGate = new CountDownLatch(1);
		List<Future<Integer>> futures = new ArrayList<>();
		for (int i = 0; i < attempts; i++) {
			futures.add(executor.submit(() -> {
				startGate.await();
				return mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel"))
						.andReturn().getResponse().getStatus();
			}));
		}

		startGate.countDown();
		for (Future<Integer> future : futures) {
			assertThat(future.get()).isEqualTo(200);
		}
		executor.shutdown();

		assertThat(stockOf(productId)).isEqualTo(5);
	}

	@Test
	void orderForUnknownProductIsNotFoundAndLeavesNoOrderBehind() throws Exception {
		long ordersBefore = orders.count();

		placeOrder("ghost-" + UUID.randomUUID(), line(UUID.randomUUID().toString(), 1))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("PRODUCT_NOT_FOUND"));

		assertThat(orders.count()).isEqualTo(ordersBefore);
	}

	@Test
	void invalidOrdersAreRejectedWithFieldMessages() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
						.header("Idempotency-Key", "invalid-" + UUID.randomUUID())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[]}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("items"));

		mockMvc.perform(post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[{\"productId\":\"" + UUID.randomUUID() + "\",\"quantity\":1}]}"))
				.andExpect(status().isBadRequest());

		placeOrder(" ", line(UUID.randomUUID().toString(), 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_IDEMPOTENCY_KEY"));
		placeOrder("k".repeat(101), line(UUID.randomUUID().toString(), 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("INVALID_IDEMPOTENCY_KEY"));
	}

	@Test
	void unknownOrderIsNotFound() throws Exception {
		mockMvc.perform(get("/api/v1/orders/" + UUID.randomUUID())).andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/orders/" + UUID.randomUUID() + "/cancel")).andExpect(status().isNotFound());
	}

	private String createProduct(String name, long stock) throws Exception {
		String response = mockMvc.perform(post("/api/v1/inventory-items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\",\"stock\":" + stock + "}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).get("id").asText();
	}

	private ResultActions placeOrder(String key, String... lines)
			throws Exception {
		return mockMvc.perform(post("/api/v1/orders")
				.header("Idempotency-Key", key)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[" + String.join(",", lines) + "]}"));
	}

	private String line(String productId, long quantity) {
		return "{\"productId\":\"" + productId + "\",\"quantity\":" + quantity + "}";
	}

	private JsonNode json(ResultActions actions) throws Exception {
		return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
	}

	private long stockOf(String productId) {
		return inventory.findById(UUID.fromString(productId)).orElseThrow().getStock();
	}

	private long ordersContaining(String productId) {
		return jdbc.queryForObject(
				"select count(distinct order_id) from order_items where product_id = ?", Long.class,
				UUID.fromString(productId));
	}
}
