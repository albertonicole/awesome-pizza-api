package com.awesomepizza.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.awesomepizza.api.model.Order;
import com.awesomepizza.api.model.OrderItem;
import com.awesomepizza.api.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@DisplayName("OrderRepository Tests")
class OrderRepositoryTest {

	@Autowired
	private OrderRepository orderRepository;

	@Nested
	@DisplayName("findByOrderCode")
	class FindByOrderCodeTests {

		@Test
		@DisplayName("dovrebbe trovare un ordine esistente tramite codice")
		void shouldFindExistingOrderByCode() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.PENDING)
					.build();
			orderRepository.save(order);

			// When
			Optional<Order> found = orderRepository.findByOrderCode(orderCode);

			// Then
			assertThat(found).isPresent();
			assertThat(found.get().getOrderCode()).isEqualTo(orderCode);
		}

		@Test
		@DisplayName("dovrebbe restituire Optional vuoto per codice non esistente")
		void shouldReturnEmptyOptionalForNonExistentCode() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			String nonExistentCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.PENDING)
					.build();
			orderRepository.save(order);

			// When
			Optional<Order> found = orderRepository.findByOrderCode(nonExistentCode);

			// Then
			assertThat(found).isEmpty();
		}
	}

	/**
	 * Poiché H2 non supporta il lock pessimistico in modo completo,
	 * questi test non testano la logica di lock pessimistico in sé, ma verificano
	 * solo che il metodo funzioni correttamente nel contesto del repository.
	 */
	@Nested
	@DisplayName("findByOrderCodeWithLock")
	class FindByOrderCodeWithLockTests {

		@Test
		@DisplayName("dovrebbe trovare un ordine")
		void shouldFindOrderWithPessimisticLock() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.IN_PROGRESS)
					.build();
			orderRepository.save(order);

			// When
			Optional<Order> found = orderRepository.findByOrderCodeWithLock(orderCode);

			// Then
			assertThat(found).isPresent();
			assertThat(found.get().getOrderCode()).isEqualTo(orderCode);
		}

		@Test
		@DisplayName("dovrebbe restituire Optional vuoto per codice non esistente")
		void shouldReturnEmptyOptionalForNonExistentCode() {
			// When
			String nonExistentCode = UUID.randomUUID().toString();
			Optional<Order> found = orderRepository.findByOrderCodeWithLock(nonExistentCode);

			// Then
			assertThat(found).isEmpty();
		}
	}

	@Nested
	@DisplayName("findByStatusWithItemsOrderByCreatedAtAsc")
	class FindByStatusWithItemsOrderByCreatedAtAscTests {

		@Test
		@DisplayName("dovrebbe restituire ordini PENDING con items, ordinati per data creazione")
		void shouldReturnPendingOrdersWithItemsOrderedByCreatedAt() {
			// Given
			String orderCode1 = UUID.randomUUID().toString();
			String orderCode2 = UUID.randomUUID().toString();
			Order order1 = Order.builder()
					.orderCode(orderCode1)
					.customerName("Primo Cliente")
					.status(OrderStatus.PENDING)
					.createdAt(LocalDateTime.now().minusMinutes(30))
					.build();
			OrderItem item1 = OrderItem.builder().pizzaName("Margherita").quantity(2).build();
			order1.addItem(item1);

			Order order2 = Order.builder()
					.orderCode(orderCode2)
					.customerName("Secondo Cliente")
					.status(OrderStatus.PENDING)
					.createdAt(LocalDateTime.now().minusMinutes(10))
					.build();
			OrderItem item2 = OrderItem.builder().pizzaName("Diavola").quantity(1).build();
			order2.addItem(item2);

			orderRepository.save(order1);
			orderRepository.save(order2);

			// When
			List<Order> orders = orderRepository.findByStatusWithItemsOrderByCreatedAtAsc(
					OrderStatus.PENDING, PageRequest.of(0, 100));

			// Then
			assertThat(orders).hasSize(2);
			assertThat(orders.get(0).getOrderCode()).isEqualTo(orderCode1);
			assertThat(orders.get(0).getItems()).hasSize(1);
			assertThat(orders.get(0).getItems().get(0).getPizzaName()).isEqualTo("Margherita");
			assertThat(orders.get(1).getOrderCode()).isEqualTo(orderCode2);
			assertThat(orders.get(1).getItems()).hasSize(1);
			assertThat(orders.get(1).getItems().get(0).getPizzaName()).isEqualTo("Diavola");
		}

		@Test
		@DisplayName("dovrebbe rispettare la paginazione")
		void shouldRespectPagination() {
			// Given
			String[] orderCodes = new String[5];
			for (int i = 0; i < 5; i++) {
				orderCodes[i] = UUID.randomUUID().toString();
				Order order = Order.builder()
						.orderCode(orderCodes[i])
						.customerName("Cliente " + (i + 1))
						.status(OrderStatus.PENDING)
						.createdAt(LocalDateTime.now().minusMinutes(60 - (i + 1) * 10))
						.build();
				orderRepository.save(order);
			}

			// When - richiesta con limite di 3 elementi
			List<Order> orders = orderRepository.findByStatusWithItemsOrderByCreatedAtAsc(
					OrderStatus.PENDING, PageRequest.of(0, 3));

			// Then
			assertThat(orders).hasSize(3);
			assertThat(orders.get(0).getOrderCode()).isEqualTo(orderCodes[0]);
			assertThat(orders.get(2).getOrderCode()).isEqualTo(orderCodes[2]);
		}

		@Test
		@DisplayName("dovrebbe restituire lista vuota quando non ci sono ordini con lo stato richiesto")
		void shouldReturnEmptyListWhenNoOrdersWithRequestedStatus() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order completedOrder = Order.builder()
					.orderCode(orderCode)
					.customerName("Cliente")
					.status(OrderStatus.COMPLETED)
					.build();
			orderRepository.save(completedOrder);

			// When
			List<Order> orders = orderRepository.findByStatusWithItemsOrderByCreatedAtAsc(
					OrderStatus.PENDING, PageRequest.of(0, 100));

			// Then
			assertThat(orders).isEmpty();
		}

		@Test
		@DisplayName("dovrebbe filtrare solo ordini con lo stato specificato")
		void shouldFilterOnlyOrdersWithSpecifiedStatus() {
			// Given
			String pendingOrderCode = UUID.randomUUID().toString();
			String inProgressOrderCode = UUID.randomUUID().toString();
			Order pendingOrder = Order.builder()
					.orderCode(pendingOrderCode)
					.customerName("Cliente Pending")
					.status(OrderStatus.PENDING)
					.build();
			Order inProgressOrder = Order.builder()
					.orderCode(inProgressOrderCode)
					.customerName("Cliente In Progress")
					.status(OrderStatus.IN_PROGRESS)
					.build();
			orderRepository.save(pendingOrder);
			orderRepository.save(inProgressOrder);

			// When
			List<Order> pendingOrders = orderRepository.findByStatusWithItemsOrderByCreatedAtAsc(
					OrderStatus.PENDING, PageRequest.of(0, 100));

			// Then
			assertThat(pendingOrders).hasSize(1);
			assertThat(pendingOrders.get(0).getOrderCode()).isEqualTo(pendingOrderCode);
		}
	}

	@Nested
	@DisplayName("existsByStatus")
	class ExistsByStatusTests {

		@Test
		@DisplayName("dovrebbe restituire true se esiste un ordine con lo stato specificato")
		void shouldReturnTrueWhenOrderWithStatusExists() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.orderCode(orderCode)
					.customerName("Cliente")
					.status(OrderStatus.IN_PROGRESS)
					.build();
			orderRepository.save(order);

			// When
			boolean exists = orderRepository.existsByStatus(OrderStatus.IN_PROGRESS);

			// Then
			assertThat(exists).isTrue();
		}

		@Test
		@DisplayName("dovrebbe restituire false se non esiste un ordine con lo stato specificato")
		void shouldReturnFalseWhenNoOrderWithStatusExists() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.orderCode(orderCode)
					.customerName("Cliente")
					.status(OrderStatus.PENDING)
					.build();
			orderRepository.save(order);

			// When
			boolean exists = orderRepository.existsByStatus(OrderStatus.IN_PROGRESS);

			// Then
			assertThat(exists).isFalse();
		}
	}

	@Nested
	@DisplayName("countByStatus")
	class CountByStatusTests {

		@Test
		@DisplayName("dovrebbe contare correttamente gli ordini con lo stato specificato")
		void shouldCountOrdersWithSpecifiedStatus() {
			// Given
			for (int i = 1; i <= 3; i++) {
				String orderCode = UUID.randomUUID().toString();
				Order order = Order.builder()
						.orderCode(orderCode)
						.customerName("Cliente " + i)
						.status(OrderStatus.PENDING)
						.build();
				orderRepository.save(order);
			}
			String inProgressOrderCode = UUID.randomUUID().toString();
			Order inProgressOrder = Order.builder()
					.orderCode(inProgressOrderCode)
					.customerName("Cliente In Progress")
					.status(OrderStatus.IN_PROGRESS)
					.build();
			orderRepository.save(inProgressOrder);

			// When
			long pendingCount = orderRepository.countByStatus(OrderStatus.PENDING);
			long inProgressCount = orderRepository.countByStatus(OrderStatus.IN_PROGRESS);
			long completedCount = orderRepository.countByStatus(OrderStatus.COMPLETED);

			// Then
			assertThat(pendingCount).isEqualTo(3);
			assertThat(inProgressCount).isEqualTo(1);
			assertThat(completedCount).isEqualTo(0);
		}
	}

	/**
	 * Poiché H2 non supporta il lock pessimistico in modo completo,
	 * questi test non testano la logica di lock pessimistico in sé, ma verificano
	 * solo che il metodo funzioni correttamente nel contesto del repository.
	 */
	@Nested
	@DisplayName("findFirstByStatusOrderByCreatedAtAsc (with lock)")
	class FindFirstByStatusOrderByCreatedAtAscTests {

		@Test
		@DisplayName("dovrebbe restituire il primo ordine PENDING (FIFO)")
		void shouldReturnFirstPendingOrderFifo() {
			// Given
			String firstOrderCode = UUID.randomUUID().toString();
			String secondOrderCode = UUID.randomUUID().toString();
			Order firstOrder = Order.builder()
					.orderCode(firstOrderCode)
					.customerName("Primo Cliente")
					.status(OrderStatus.PENDING)
					.createdAt(LocalDateTime.now().minusMinutes(30))
					.build();
			Order secondOrder = Order.builder()
					.orderCode(secondOrderCode)
					.customerName("Secondo Cliente")
					.status(OrderStatus.PENDING)
					.createdAt(LocalDateTime.now().minusMinutes(10))
					.build();
			orderRepository.save(firstOrder);
			orderRepository.save(secondOrder);

			// When
			Optional<Order> found = orderRepository.findFirstByStatusOrderByCreatedAtAsc(OrderStatus.PENDING);

			// Then
			assertThat(found).isPresent();
			assertThat(found.get().getOrderCode()).isEqualTo(firstOrderCode);
			assertThat(found.get().getCustomerName()).isEqualTo("Primo Cliente");
		}

		@Test
		@DisplayName("dovrebbe restituire Optional vuoto se non ci sono ordini con lo stato richiesto")
		void shouldReturnEmptyOptionalWhenNoOrdersWithRequestedStatus() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order completedOrder = Order.builder()
					.orderCode(orderCode)
					.customerName("Cliente")
					.status(OrderStatus.COMPLETED)
					.build();
			orderRepository.save(completedOrder);

			// When
			Optional<Order> found = orderRepository.findFirstByStatusOrderByCreatedAtAsc(OrderStatus.PENDING);

			// Then
			assertThat(found).isEmpty();
		}

		@Test
		@DisplayName("dovrebbe ignorare ordini con stato diverso")
		void shouldIgnoreOrdersWithDifferentStatus() {
			// Given
			String inProgressOrderCode = UUID.randomUUID().toString();
			String pendingOrderCode = UUID.randomUUID().toString();
			Order inProgressOrder = Order.builder()
					.orderCode(inProgressOrderCode)
					.customerName("Cliente In Progress")
					.status(OrderStatus.IN_PROGRESS)
					.createdAt(LocalDateTime.now().minusHours(1))
					.build();
			Order pendingOrder = Order.builder()
					.orderCode(pendingOrderCode)
					.customerName("Cliente Pending")
					.status(OrderStatus.PENDING)
					.createdAt(LocalDateTime.now().minusMinutes(10))
					.build();
			orderRepository.save(inProgressOrder);
			orderRepository.save(pendingOrder);

			// When
			Optional<Order> found = orderRepository.findFirstByStatusOrderByCreatedAtAsc(OrderStatus.PENDING);

			// Then
			assertThat(found).isPresent();
			assertThat(found.get().getOrderCode()).isEqualTo(pendingOrderCode);
		}
	}
}
