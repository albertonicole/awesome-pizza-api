package com.awesomepizza.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.awesomepizza.api.dto.CreateOrderRequest;
import com.awesomepizza.api.dto.OrderItemRequest;
import com.awesomepizza.api.dto.OrderResponse;
import com.awesomepizza.api.exception.InvalidOrderStateException;
import com.awesomepizza.api.exception.NoOrdersInQueueException;
import com.awesomepizza.api.exception.OrderAlreadyInProgressException;
import com.awesomepizza.api.model.OrderStatus;
import com.awesomepizza.api.repository.OrderRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Test di concorrenza per OrderService.
 * Utilizza Testcontainers per avviare automaticamente un'istanza PostgreSQL.
 * H2 non supporta correttamente il pessimistic locking.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("testcontainers")
@DisplayName("OrderService Concurrency Tests")
class OrderServiceConcurrencyTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

	@Autowired
	private OrderService orderService;

	@Autowired
	private OrderRepository orderRepository;

	@BeforeEach
	void setUp() {
		orderRepository.deleteAll();
	}

	@Nested
	@DisplayName("takeNextOrder concurrency")
	class TakeNextOrderConcurrencyTests {

		@Test
		@DisplayName("dovrebbe permettere un solo ordine IN_PROGRESS alla volta")
		void shouldAllowOnlyOneOrderInProgressAtATime() throws InterruptedException {
			// Given: creo 5 ordini
			int numberOfOrders = 5;
			int numberOfWorkers = 5;

			for (int i = 0; i < numberOfOrders; i++) {
				CreateOrderRequest request = CreateOrderRequest.builder()
						.customerName("Cliente " + i)
						.items(List.of(OrderItemRequest.builder()
								.pizzaName("Margherita")
								.quantity(1)
								.build()))
						.build();
				orderService.createOrder(request);
			}

			// When: 5 pizzaioli provano a prendere ordini contemporaneamente
			ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch endLatch = new CountDownLatch(numberOfWorkers);

			Set<String> takenOrderCodes = ConcurrentHashMap.newKeySet();
			AtomicInteger successCount = new AtomicInteger(0);
			AtomicInteger alreadyInProgressCount = new AtomicInteger(0);

			for (int i = 0; i < numberOfWorkers; i++) {
				executor.submit(() -> {
					try {
						startLatch.await(); // Aspetta che tutti i thread siano pronti
						OrderResponse response = orderService.takeNextOrder();
						takenOrderCodes.add(response.getOrderCode());
						successCount.incrementAndGet();
					} catch (OrderAlreadyInProgressException e) {
						alreadyInProgressCount.incrementAndGet();
					} catch (Exception ignored) {
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown(); // Avvia tutti i thread contemporaneamente
			endLatch.await(10, TimeUnit.SECONDS);
			executor.shutdown();

			// Then: solo un ordine deve essere preso (vincolo di un solo IN_PROGRESS)
			// Gli altri worker ricevono OrderAlreadyInProgressException perché
			// il vincolo impedisce di avere più di un ordine IN_PROGRESS alla volta
			assertThat(successCount.get()).isEqualTo(1);
			assertThat(takenOrderCodes).hasSize(1);
			assertThat(alreadyInProgressCount.get()).isEqualTo(numberOfWorkers - 1);
		}

		@Test
		@DisplayName("dovrebbe permettere di prendere un nuovo ordine dopo il completamento")
		void shouldAllowNextOrderAfterCompletion() {
			// Given: creo 3 ordini PENDING
			for (int i = 0; i < 3; i++) {
				CreateOrderRequest request = CreateOrderRequest.builder()
						.customerName("Cliente " + i)
						.items(List.of(OrderItemRequest.builder()
								.pizzaName("Margherita")
								.quantity(1)
								.build()))
						.build();
				orderService.createOrder(request);
			}

			// When: Worker prende ordine 1 → IN_PROGRESS
			OrderResponse firstOrder = orderService.takeNextOrder();
			assertThat(firstOrder.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);

			// Worker completa ordine 1 → COMPLETED
			OrderResponse completedOrder = orderService.completeOrder(firstOrder.getOrderCode());
			assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

			// Altro worker prende ordine 2 → deve funzionare!
			OrderResponse secondOrder = orderService.takeNextOrder();

			// Then: ordine 2 è IN_PROGRESS e diverso dal primo
			assertThat(secondOrder.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
			assertThat(secondOrder.getOrderCode()).isNotEqualTo(firstOrder.getOrderCode());

			// Verifica stato finale nel database
			long inProgressCount = orderRepository.countByStatus(OrderStatus.IN_PROGRESS);
			long completedCount = orderRepository.countByStatus(OrderStatus.COMPLETED);
			assertThat(inProgressCount).isEqualTo(1);
			assertThat(completedCount).isEqualTo(1);
		}
	}

	@Nested
	@DisplayName("completeOrder concurrency")
	class CompleteOrderConcurrencyTests {

		@Test
		@DisplayName("dovrebbe permettere il completamento di un ordine da un solo thread")
		void shouldAllowCompletionByOnlyOneThread() throws InterruptedException {
			// Given: creo un ordine e lo metto in lavorazione
			CreateOrderRequest request = CreateOrderRequest.builder()
					.customerName("Cliente Test")
					.items(List.of(OrderItemRequest.builder()
							.pizzaName("Quattro Stagioni")
							.quantity(1)
							.build()))
					.build();
			orderService.createOrder(request);
			OrderResponse inProgress = orderService.takeNextOrder();
			String orderCode = inProgress.getOrderCode();

			// When: 5 thread provano a completare lo stesso ordine
			int numberOfWorkers = 5;
			ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch endLatch = new CountDownLatch(numberOfWorkers);

			AtomicInteger successCount = new AtomicInteger(0);
			AtomicInteger illegalStateCount = new AtomicInteger(0);
			List<OrderStatus> completedStatuses = Collections.synchronizedList(new ArrayList<>());

			for (int i = 0; i < numberOfWorkers; i++) {
				executor.submit(() -> {
					try {
						startLatch.await();
						OrderResponse response = orderService.completeOrder(orderCode);
						completedStatuses.add(response.getStatus());
						successCount.incrementAndGet();
				} catch (InvalidOrderStateException e) {
					illegalStateCount.incrementAndGet();
					} catch (Exception e) {
						// Altri errori
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			endLatch.await(10, TimeUnit.SECONDS);
			executor.shutdown();

			// Then: solo un thread deve riuscire a completare l'ordine
			assertThat(successCount.get()).isEqualTo(1);
			assertThat(illegalStateCount.get()).isEqualTo(numberOfWorkers - 1);
			assertThat(completedStatuses).containsExactly(OrderStatus.COMPLETED);
		}
	}

	@Nested
	@DisplayName("mixed operations concurrency")
	class MixedOperationsConcurrencyTests {

		@Test
		@DisplayName("dovrebbe mantenere consistenza con cicli take-complete concorrenti")
		void shouldMaintainConsistencyWithMixedOperations() throws InterruptedException {
			// Given: creo 5 ordini
			int numberOfOrders = 5;

			for (int i = 0; i < numberOfOrders; i++) {
				CreateOrderRequest request = CreateOrderRequest.builder()
						.customerName("Cliente " + i)
						.items(List.of(OrderItemRequest.builder()
								.pizzaName("Capricciosa")
								.quantity(1)
								.build()))
						.build();
				orderService.createOrder(request);
			}

			// When: 5 worker eseguono cicli take→complete in parallelo
			int numberOfWorkers = 5;
			ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch endLatch = new CountDownLatch(numberOfWorkers);

			AtomicInteger takeSuccessCount = new AtomicInteger(0);
			AtomicInteger completeSuccessCount = new AtomicInteger(0);
			Set<String> processedOrders = ConcurrentHashMap.newKeySet();

			for (int i = 0; i < numberOfWorkers; i++) {
				executor.submit(() -> {
					try {
						startLatch.await();
						// Ogni worker tenta più cicli take→complete
						for (int cycle = 0; cycle < 3; cycle++) {
							try {
								OrderResponse taken = orderService.takeNextOrder();
								takeSuccessCount.incrementAndGet();
								processedOrders.add(taken.getOrderCode());

								// Completa immediatamente l'ordine preso
								orderService.completeOrder(taken.getOrderCode());
								completeSuccessCount.incrementAndGet();
							} catch (OrderAlreadyInProgressException | NoOrdersInQueueException e) {
								// Atteso: altro worker ha l'ordine o coda vuota
							} catch (InvalidOrderStateException e) {
								// Atteso: ordine già completato da altro thread
							}
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						endLatch.countDown();
					}
				});
			}

			startLatch.countDown();
			endLatch.await(30, TimeUnit.SECONDS);
			executor.shutdown();

			// Then: verifiche di consistenza
			// 1. Ogni ordine preso con successo deve essere stato completato
			assertThat(completeSuccessCount.get()).isEqualTo(takeSuccessCount.get());

			// 2. Non possono essere stati processati più ordini di quelli creati
			assertThat(processedOrders.size()).isLessThanOrEqualTo(numberOfOrders);

			// 3. Verifica stato finale nel database: nessun ordine deve essere IN_PROGRESS
			long inProgressCount = orderRepository.countByStatus(OrderStatus.IN_PROGRESS);
			assertThat(inProgressCount).isZero();

			// 4. Il totale degli ordini (PENDING + COMPLETED) deve essere uguale a numberOfOrders
			long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
			long completedCount = orderRepository.countByStatus(OrderStatus.COMPLETED);
			assertThat(pendingCount + completedCount).isEqualTo(numberOfOrders);

			// 5. Gli ordini completati devono corrispondere ai take riusciti
			assertThat(completedCount).isEqualTo(takeSuccessCount.get());
		}
	}
}
