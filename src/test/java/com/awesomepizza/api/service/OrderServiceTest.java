package com.awesomepizza.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.awesomepizza.api.dto.CreateOrderRequest;
import com.awesomepizza.api.dto.OrderItemRequest;
import com.awesomepizza.api.dto.OrderResponse;
import com.awesomepizza.api.dto.OrderStatusResponse;
import com.awesomepizza.api.exception.InvalidOrderStateException;
import com.awesomepizza.api.exception.NoOrdersInQueueException;
import com.awesomepizza.api.exception.OrderAlreadyInProgressException;
import com.awesomepizza.api.exception.OrderNotFoundException;
import com.awesomepizza.api.mapper.OrderMapper;
import com.awesomepizza.api.model.Order;
import com.awesomepizza.api.model.OrderStatus;
import com.awesomepizza.api.repository.OrderRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService Unit Tests")
class OrderServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderMapper orderMapper;

	@InjectMocks
	private OrderServiceImpl orderService;

	@Captor
	private ArgumentCaptor<Order> orderCaptor;

	private CreateOrderRequest validRequest;
	private Order sampleOrder;
	private OrderResponse sampleResponse;

	@BeforeEach
	void setUp() {
		validRequest = CreateOrderRequest.builder()
				.customerName("Mario Rossi")
				.items(List.of(
						OrderItemRequest.builder().pizzaName("Margherita").quantity(2).build(),
						OrderItemRequest.builder().pizzaName("Diavola").quantity(1).build()
				))
				.build();

		sampleOrder = Order.builder()
				.id(1L)
				.orderCode("test-order-code")
				.customerName("Mario Rossi")
				.status(OrderStatus.PENDING)
				.build();

		sampleResponse = OrderResponse.builder()
				.orderCode("test-order-code")
				.customerName("Mario Rossi")
				.status(OrderStatus.PENDING)
				.build();
	}

	@Nested
	@DisplayName("createOrder")
	class CreateOrderTests {

		@Test
		@DisplayName("dovrebbe salvare l'ordine nel repository")
		void shouldSaveOrderInRepository() {
			// Given
			when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
			when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(sampleResponse);

			// When
			orderService.createOrder(validRequest);

			// Then
			verify(orderRepository).save(orderCaptor.capture());
			Order savedOrder = orderCaptor.getValue();
			assertThat(savedOrder.getCustomerName()).isEqualTo("Mario Rossi");
			assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
		}

		@Test
		@DisplayName("dovrebbe generare un codice ordine univoco nel formato UUID")
		void shouldGenerateUniqueOrderCode() {
			// Given
			when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
			when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(sampleResponse);

			// When
			orderService.createOrder(validRequest);

			// Then
			verify(orderRepository).save(orderCaptor.capture());
			Order savedOrder = orderCaptor.getValue();
			String orderCode = savedOrder.getOrderCode();
			assertThat(orderCode).isNotNull().isNotBlank();
			assertThat(orderCode).hasSize(36);
			assertThat(orderCode).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
		}

		@Test
		@DisplayName("dovrebbe aggiungere tutti gli item all'ordine")
		void shouldAddAllItemsToOrder() {
			// Given
			when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
			when(orderMapper.toOrderResponse(any(Order.class))).thenReturn(sampleResponse);

			// When
			orderService.createOrder(validRequest);

			// Then
			verify(orderRepository).save(orderCaptor.capture());
			Order savedOrder = orderCaptor.getValue();
			assertThat(savedOrder.getItems()).hasSize(2);
			assertThat(savedOrder.getItems().get(0).getPizzaName()).isEqualTo("Margherita");
			assertThat(savedOrder.getItems().get(1).getPizzaName()).isEqualTo("Diavola");
		}

		@Test
		@DisplayName("dovrebbe mappare l'ordine salvato in response")
		void shouldMapSavedOrderToResponse() {
			// Given
			when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
			when(orderMapper.toOrderResponse(sampleOrder)).thenReturn(sampleResponse);

			// When
			OrderResponse response = orderService.createOrder(validRequest);

			// Then
			verify(orderMapper).toOrderResponse(sampleOrder);
			assertThat(response).isEqualTo(sampleResponse);
		}
	}

	@Nested
	@DisplayName("getOrderByCode")
	class GetOrderByCodeTests {

		@Test
		@DisplayName("dovrebbe restituire l'ordine mappato quando trovato")
		void shouldReturnMappedOrderWhenFound() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(sampleOrder));
			when(orderMapper.toOrderResponse(sampleOrder)).thenReturn(sampleResponse);

			// When
			OrderResponse response = orderService.getOrderByCode(orderCode);

			// Then
			verify(orderRepository).findByOrderCode(orderCode);
			verify(orderMapper).toOrderResponse(sampleOrder);
			assertThat(response).isEqualTo(sampleResponse);
		}

		@Test
		@DisplayName("dovrebbe lanciare OrderNotFoundException quando ordine non trovato")
		void shouldThrowOrderNotFoundExceptionWhenNotFound() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.empty());

			// When/Then
			assertThatThrownBy(() -> orderService.getOrderByCode(orderCode))
					.isInstanceOf(OrderNotFoundException.class)
					.hasMessageContaining(orderCode);

			verify(orderMapper, never()).toOrderResponse(any());
		}
	}

	@Nested
	@DisplayName("getOrderStatusByCode")
	class GetOrderStatusByCodeTests {

		@Test
		@DisplayName("dovrebbe restituire lo stato dell'ordine quando trovato")
		void shouldReturnOrderStatusWhenFound() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			OrderStatusResponse statusResponse = OrderStatusResponse.builder()
					.status(OrderStatus.PENDING)
					.build();

			when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(sampleOrder));
			when(orderMapper.toOrderStatusResponse(sampleOrder)).thenReturn(statusResponse);

			// When
			OrderStatusResponse response = orderService.getOrderStatusByCode(orderCode);

			// Then
			verify(orderRepository).findByOrderCode(orderCode);
			verify(orderMapper).toOrderStatusResponse(sampleOrder);
			assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
		}

		@Test
		@DisplayName("dovrebbe lanciare OrderNotFoundException quando ordine non trovato")
		void shouldThrowOrderNotFoundExceptionWhenNotFound() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			when(orderRepository.findByOrderCode(orderCode)).thenReturn(Optional.empty());

			// When/Then
			assertThatThrownBy(() -> orderService.getOrderStatusByCode(orderCode))
					.isInstanceOf(OrderNotFoundException.class)
					.hasMessageContaining(orderCode);

			verify(orderMapper, never()).toOrderStatusResponse(any());
		}
	}

	@Nested
	@DisplayName("getOrderQueue")
	class GetOrderQueueTests {

		@Test
		@DisplayName("dovrebbe restituire una pagina di ordini PENDING")
		void shouldReturnPagedPendingOrders() {
			// Given
			Pageable pageable = PageRequest.of(0, 20);
			Order order1 = Order.builder().id(1L).orderCode("order-1").status(OrderStatus.PENDING).build();
			Order order2 = Order.builder().id(2L).orderCode("order-2").status(OrderStatus.PENDING).build();
			OrderResponse response1 = OrderResponse.builder().orderCode("order-1").status(OrderStatus.PENDING).build();
			OrderResponse response2 = OrderResponse.builder().orderCode("order-2").status(OrderStatus.PENDING).build();

			Page<Long> idPage = new PageImpl<>(List.of(1L, 2L), pageable, 2);
			when(orderRepository.findIdsByStatusOrderByCreatedAtAsc(OrderStatus.PENDING, pageable))
					.thenReturn(idPage);
			when(orderRepository.findByIdsWithItems(List.of(1L, 2L)))
					.thenReturn(List.of(order1, order2));
			when(orderMapper.toOrderResponse(order1)).thenReturn(response1);
			when(orderMapper.toOrderResponse(order2)).thenReturn(response2);

			// When
			Page<OrderResponse> result = orderService.getOrderQueue(pageable);

			// Then
			assertThat(result.getContent()).hasSize(2);
			assertThat(result.getTotalElements()).isEqualTo(2);
			assertThat(result.getContent().get(0).getOrderCode()).isEqualTo("order-1");
		}

		@Test
		@DisplayName("dovrebbe restituire pagina vuota quando non ci sono ordini")
		void shouldReturnEmptyPageWhenNoOrders() {
			// Given
			Pageable pageable = PageRequest.of(0, 20);
			when(orderRepository.findIdsByStatusOrderByCreatedAtAsc(OrderStatus.PENDING, pageable))
					.thenReturn(Page.empty(pageable));

			// When
			Page<OrderResponse> result = orderService.getOrderQueue(pageable);

			// Then
			assertThat(result.getContent()).isEmpty();
			assertThat(result.getTotalElements()).isZero();
			verify(orderRepository, never()).findByIdsWithItems(any());
		}
	}

	@Nested
	@DisplayName("takeNextOrder")
	class TakeNextOrderTests {

		@Test
		@DisplayName("dovrebbe cambiare lo stato da PENDING a IN_PROGRESS")
		void shouldChangeStatusFromPendingToInProgress() {
			// Given
			Order pendingOrder = Order.builder()
					.id(1L)
					.orderCode("order-1")
					.status(OrderStatus.PENDING)
					.build();
			Order inProgressOrder = Order.builder()
					.id(1L)
					.orderCode("order-1")
					.status(OrderStatus.IN_PROGRESS)
					.build();
			OrderResponse expectedResponse = OrderResponse.builder()
					.orderCode("order-1")
					.status(OrderStatus.IN_PROGRESS)
					.build();

			// Trova il primo PENDING
			when(orderRepository.findFirstByStatusOrderByCreatedAtAsc(eq(OrderStatus.PENDING)))
					.thenReturn(Optional.of(pendingOrder));
			// Double-check: nessun IN_PROGRESS
			when(orderRepository.existsByStatus(eq(OrderStatus.IN_PROGRESS)))
					.thenReturn(false);
			when(orderRepository.save(argThat(o -> o.getStatus() == OrderStatus.IN_PROGRESS)))
					.thenReturn(inProgressOrder);
			when(orderMapper.toOrderResponse(inProgressOrder)).thenReturn(expectedResponse);

			// When
			OrderResponse response = orderService.takeNextOrder();

			// Then
			verify(orderRepository).save(orderCaptor.capture());
			Order savedOrder = orderCaptor.getValue();
			assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
			assertThat(savedOrder.getOrderCode()).isEqualTo("order-1");
			assertThat(response.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
			assertThat(response.getOrderCode()).isEqualTo("order-1");
		}

		@Test
		@DisplayName("dovrebbe lanciare NoOrdersInQueueException se la coda è vuota")
		void shouldThrowNoOrdersInQueueExceptionWhenQueueIsEmpty() {
			// Given - nessun ordine PENDING
			when(orderRepository.findFirstByStatusOrderByCreatedAtAsc(eq(OrderStatus.PENDING)))
					.thenReturn(Optional.empty());

			// When/Then
			assertThatThrownBy(() -> orderService.takeNextOrder())
					.isInstanceOf(NoOrdersInQueueException.class);

			// Non dovrebbe mai arrivare al double-check
			verify(orderRepository, never()).existsByStatus(any());
			verify(orderRepository, never()).save(any());
		}

		@Test
		@DisplayName("dovrebbe lanciare OrderAlreadyInProgressException se esiste già un ordine in lavorazione")
		void shouldThrowOrderAlreadyInProgressExceptionWhenAnotherOrderIsInProgress() {
			// Given
			Order pendingOrder = Order.builder()
					.id(1L)
					.orderCode("order-1")
					.status(OrderStatus.PENDING)
					.build();

			// Prima trova un PENDING
			when(orderRepository.findFirstByStatusOrderByCreatedAtAsc(eq(OrderStatus.PENDING)))
					.thenReturn(Optional.of(pendingOrder));
			// Poi il double-check trova un IN_PROGRESS
			when(orderRepository.existsByStatus(eq(OrderStatus.IN_PROGRESS)))
					.thenReturn(true);

			// When/Then
			assertThatThrownBy(() -> orderService.takeNextOrder())
					.isInstanceOf(OrderAlreadyInProgressException.class)
					.hasMessageContaining("già presente un ordine in lavorazione");

			verify(orderRepository, never()).save(any());
		}
	}

	@Nested
	@DisplayName("completeOrder")
	class CompleteOrderTests {

		@Test
		@DisplayName("dovrebbe completare un ordine in lavorazione")
		void shouldCompleteOrderInProgress() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order inProgressOrder = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.IN_PROGRESS)
					.build();
			Order completedOrder = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.COMPLETED)
					.build();
			OrderResponse expectedResponse = OrderResponse.builder()
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.COMPLETED)
					.build();

			when(orderRepository.findByOrderCodeWithLock(eq(orderCode))).thenReturn(Optional.of(inProgressOrder));
			when(orderRepository.save(argThat(o -> o.getStatus() == OrderStatus.COMPLETED)))
					.thenReturn(completedOrder);
			when(orderMapper.toOrderResponse(completedOrder)).thenReturn(expectedResponse);

			// When
			OrderResponse response = orderService.completeOrder(orderCode);

			// Then
			verify(orderRepository).findByOrderCodeWithLock(eq(orderCode));
			verify(orderRepository).save(orderCaptor.capture());
			Order savedOrder = orderCaptor.getValue();
			assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
			assertThat(savedOrder.getOrderCode()).isEqualTo(orderCode);
			assertThat(response.getStatus()).isEqualTo(OrderStatus.COMPLETED);
			assertThat(response.getOrderCode()).isEqualTo(orderCode);
		}

		@Test
		@DisplayName("dovrebbe lanciare InvalidOrderStateException se l'ordine non è IN_PROGRESS")
		void shouldThrowInvalidOrderStateExceptionWhenOrderNotInProgress() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order pendingOrder = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.status(OrderStatus.PENDING)
					.build();

			when(orderRepository.findByOrderCodeWithLock(eq(orderCode))).thenReturn(Optional.of(pendingOrder));

			// When/Then
			assertThatThrownBy(() -> orderService.completeOrder(orderCode))
					.isInstanceOf(InvalidOrderStateException.class)
					.hasMessageContaining("IN_PROGRESS")
					.hasMessageContaining("PENDING");

			verify(orderRepository, never()).save(any());
		}

		@Test
		@DisplayName("dovrebbe lanciare InvalidOrderStateException per ordine già completato")
		void shouldThrowInvalidOrderStateExceptionWhenOrderAlreadyCompleted() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order completedOrder = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.status(OrderStatus.COMPLETED)
					.build();

			when(orderRepository.findByOrderCodeWithLock(eq(orderCode))).thenReturn(Optional.of(completedOrder));

			// When/Then
			assertThatThrownBy(() -> orderService.completeOrder(orderCode))
					.isInstanceOf(InvalidOrderStateException.class)
					.hasMessageContaining("IN_PROGRESS")
					.hasMessageContaining("COMPLETED");

			verify(orderRepository, never()).save(any());
		}

		@Test
		@DisplayName("dovrebbe lanciare OrderNotFoundException per ordine non trovato")
		void shouldThrowOrderNotFoundExceptionWhenNotFound() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			when(orderRepository.findByOrderCodeWithLock(eq(orderCode))).thenReturn(Optional.empty());

			// When/Then
			assertThatThrownBy(() -> orderService.completeOrder(orderCode))
					.isInstanceOf(OrderNotFoundException.class)
					.hasMessageContaining(orderCode);

			verify(orderRepository).findByOrderCodeWithLock(eq(orderCode));
			verify(orderRepository, never()).save(any());
		}
	}
}
