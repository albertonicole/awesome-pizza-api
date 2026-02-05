package com.awesomepizza.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.awesomepizza.api.dto.OrderItemResponse;
import com.awesomepizza.api.dto.OrderResponse;
import com.awesomepizza.api.dto.OrderStatusResponse;
import com.awesomepizza.api.model.Order;
import com.awesomepizza.api.model.OrderItem;
import com.awesomepizza.api.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("OrderMapper Tests")
class OrderMapperTest {

	private OrderMapper orderMapper;

	@BeforeEach
	void setUp() {
		orderMapper = new OrderMapperImpl();
	}

	@Nested
	@DisplayName("toOrderResponse")
	class ToOrderResponseTests {

		@Test
		@DisplayName("dovrebbe mappare correttamente tutti i campi dell'ordine")
		void shouldMapAllOrderFields() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			LocalDateTime createdAt = LocalDateTime.of(2026, 2, 3, 10, 30, 0);
			Order order = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.PENDING)
					.createdAt(createdAt)
					.build();

			// When
			OrderResponse response = orderMapper.toOrderResponse(order);

			// Then
			assertThat(response.getOrderCode()).isEqualTo(orderCode);
			assertThat(response.getCustomerName()).isEqualTo("Mario Rossi");
			assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
			assertThat(response.getCreatedAt()).isEqualTo(createdAt);
		}

		@Test
		@DisplayName("dovrebbe mappare correttamente gli items dell'ordine")
		void shouldMapOrderItems() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.PENDING)
					.createdAt(LocalDateTime.now())
					.build();

			OrderItem item1 = OrderItem.builder()
					.id(1L)
					.pizzaName("Margherita")
					.quantity(2)
					.build();
			OrderItem item2 = OrderItem.builder()
					.id(2L)
					.pizzaName("Diavola")
					.quantity(1)
					.build();

			order.addItem(item1);
			order.addItem(item2);

			// When
			OrderResponse response = orderMapper.toOrderResponse(order);

			// Then
			assertThat(response.getItems()).hasSize(2);
			assertThat(response.getItems().get(0).getPizzaName()).isEqualTo("Margherita");
			assertThat(response.getItems().get(0).getQuantity()).isEqualTo(2);
			assertThat(response.getItems().get(1).getPizzaName()).isEqualTo("Diavola");
			assertThat(response.getItems().get(1).getQuantity()).isEqualTo(1);
		}

		@Test
		@DisplayName("dovrebbe gestire ordine con lista items vuota")
		void shouldHandleEmptyItemsList() {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.PENDING)
					.createdAt(LocalDateTime.now())
					.build();

			// When
			OrderResponse response = orderMapper.toOrderResponse(order);

			// Then
			assertThat(response.getItems()).isEmpty();
		}

		@ParameterizedTest(name = "dovrebbe mappare correttamente lo stato {0}")
		@EnumSource(OrderStatus.class)
		@DisplayName("dovrebbe mappare tutti gli stati degli ordini")
		void shouldMapAllOrderStatuses(OrderStatus status) {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.customerName("Cliente")
					.status(status)
					.createdAt(LocalDateTime.now())
					.build();

			// When
			OrderResponse response = orderMapper.toOrderResponse(order);

			// Then
			assertThat(response.getStatus()).isEqualTo(status);
		}

		@Test
		@DisplayName("dovrebbe gestire ordine null restituendo null")
		void shouldHandleNullOrder() {
			// Given/When
			OrderResponse response = orderMapper.toOrderResponse(null);

			// Then
			assertThat(response).isNull();
		}
	}

	@Nested
	@DisplayName("toOrderItemResponse")
	class ToOrderItemResponseTests {

		@Test
		@DisplayName("dovrebbe mappare correttamente un OrderItem")
		void shouldMapOrderItem() {
			// Given
			OrderItem item = OrderItem.builder()
					.id(1L)
					.pizzaName("Quattro Stagioni")
					.quantity(3)
					.build();

			// When
			OrderItemResponse response = orderMapper.toOrderItemResponse(item);

			// Then
			assertThat(response.getPizzaName()).isEqualTo("Quattro Stagioni");
			assertThat(response.getQuantity()).isEqualTo(3);
		}

		@Test
		@DisplayName("dovrebbe gestire item null restituendo null")
		void shouldHandleNullItem() {
			// Given/When
			OrderItemResponse response = orderMapper.toOrderItemResponse(null);

			// Then
			assertThat(response).isNull();
		}
	}

	@Nested
	@DisplayName("toOrderStatusResponse")
	class ToOrderStatusResponseTests {

		@ParameterizedTest(name = "dovrebbe mappare correttamente lo stato {0}")
		@EnumSource(OrderStatus.class)
		@DisplayName("dovrebbe mappare lo stato dell'ordine")
		void shouldMapOrderStatus(OrderStatus status) {
			// Given
			String orderCode = UUID.randomUUID().toString();
			Order order = Order.builder()
					.id(1L)
					.orderCode(orderCode)
					.customerName("Cliente")
					.status(status)
					.createdAt(LocalDateTime.now())
					.build();

			// When
			OrderStatusResponse response = orderMapper.toOrderStatusResponse(order);

			// Then
			assertThat(response).isNotNull();
			assertThat(response.getStatus()).isEqualTo(status);
		}

		@Test
		@DisplayName("dovrebbe gestire ordine null restituendo null")
		void shouldHandleNullOrder() {
			// Given/When
			OrderStatusResponse response = orderMapper.toOrderStatusResponse(null);

			// Then
			assertThat(response).isNull();
		}
	}
}
