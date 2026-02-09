package com.awesomepizza.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.awesomepizza.api.dto.CreateOrderRequest;
import com.awesomepizza.api.dto.OrderItemRequest;
import com.awesomepizza.api.dto.OrderItemResponse;
import com.awesomepizza.api.dto.OrderResponse;
import com.awesomepizza.api.dto.OrderStatusResponse;
import com.awesomepizza.api.exception.InvalidOrderStateException;
import com.awesomepizza.api.exception.NoOrdersInQueueException;
import com.awesomepizza.api.exception.OrderNotFoundException;
import com.awesomepizza.api.model.OrderStatus;
import com.awesomepizza.api.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController Tests")
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OrderService orderService;

	@Nested
	@DisplayName("POST /api/orders")
	class CreateOrderTests {

		@Test
		@DisplayName("dovrebbe creare un ordine e restituire 201")
		void shouldCreateOrderAndReturn201() throws Exception {
			// Given
			String orderCode = UUID.randomUUID().toString();
			CreateOrderRequest request = CreateOrderRequest.builder()
					.customerName("Mario Rossi")
					.items(List.of(OrderItemRequest.builder().pizzaName("Margherita").quantity(2).build()))
					.build();

			OrderResponse response = OrderResponse.builder()
					.orderCode(orderCode)
					.customerName("Mario Rossi")
					.status(OrderStatus.PENDING)
					.createdAt(LocalDateTime.now())
					.items(List.of(OrderItemResponse.builder()
							.pizzaName("Margherita").quantity(2).build()))
					.build();

			when(orderService.createOrder(any(CreateOrderRequest.class))).thenReturn(response);

			// When/Then
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.orderCode", is(orderCode)))
					.andExpect(jsonPath("$.customerName", is("Mario Rossi")))
					.andExpect(jsonPath("$.status", is("PENDING")));
		}

		@Test
		@DisplayName("dovrebbe restituire 400 se il nome cliente è vuoto")
		void shouldReturn400WhenCustomerNameIsEmpty() throws Exception {
			// Given
			CreateOrderRequest request = CreateOrderRequest.builder()
					.customerName("")
					.items(List.of(OrderItemRequest.builder().pizzaName("Margherita").quantity(1).build()))
					.build();

			// When/Then
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").exists())
					.andExpect(jsonPath("$.status").value(400));
		}

		@Test
		@DisplayName("dovrebbe restituire 400 se non ci sono pizze")
		void shouldReturn400WhenNoItems() throws Exception {
			// Given
			CreateOrderRequest request = CreateOrderRequest.builder()
					.customerName("Mario Rossi")
					.items(List.of())
					.build();

			// When/Then
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").exists())
					.andExpect(jsonPath("$.status").value(400));
		}

		@Test
		@DisplayName("dovrebbe restituire 400 se il nome cliente supera 255 caratteri")
		void shouldReturn400WhenCustomerNameExceeds255Chars() throws Exception {
			// Given - nome cliente di 256 caratteri
			String longName = "A".repeat(256);
			CreateOrderRequest request = CreateOrderRequest.builder()
					.customerName(longName)
					.items(List.of(OrderItemRequest.builder().pizzaName("Margherita").quantity(1).build()))
					.build();

			// When/Then
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").exists())
					.andExpect(jsonPath("$.status").value(400));
		}

		@Test
		@DisplayName("dovrebbe restituire 400 se il nome pizza supera 100 caratteri")
		void shouldReturn400WhenPizzaNameExceeds100Chars() throws Exception {
			// Given - nome pizza di 101 caratteri
			String longPizzaName = "P".repeat(101);
			CreateOrderRequest request = CreateOrderRequest.builder()
					.customerName("Mario Rossi")
					.items(List.of(OrderItemRequest.builder().pizzaName(longPizzaName).quantity(1).build()))
					.build();

			// When/Then
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").exists())
					.andExpect(jsonPath("$.status").value(400));
		}

		@Test
		@DisplayName("dovrebbe restituire 400 se la quantità è 0")
		void shouldReturn400WhenQuantityIsZero() throws Exception {
			// Given
			CreateOrderRequest request = CreateOrderRequest.builder()
					.customerName("Mario Rossi")
					.items(List.of(OrderItemRequest.builder().pizzaName("Margherita").quantity(0).build()))
					.build();

			// When/Then
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").exists())
					.andExpect(jsonPath("$.status").value(400));
		}

		@Test
		@DisplayName("dovrebbe restituire 400 se la quantità supera 100")
		void shouldReturn400WhenQuantityExceeds100() throws Exception {
			// Given
			CreateOrderRequest request = CreateOrderRequest.builder()
					.customerName("Mario Rossi")
					.items(List.of(OrderItemRequest.builder().pizzaName("Margherita").quantity(101).build()))
					.build();

			// When/Then
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").exists())
					.andExpect(jsonPath("$.status").value(400));
		}
	}

	@Nested
	@DisplayName("GET /api/orders/{orderCode}")
	class GetOrderTests {

		@Test
		@DisplayName("dovrebbe restituire l'ordine con il codice specificato")
		void shouldReturnOrderByCode() throws Exception {
			// Given
			String orderCode = UUID.randomUUID().toString();
			LocalDateTime createdAt = LocalDateTime.of(2026, 2, 3, 10, 30, 0);
			OrderResponse response = OrderResponse.builder()
					.orderCode(orderCode)
					.customerName("Luigi Verdi")
					.status(OrderStatus.IN_PROGRESS)
					.createdAt(createdAt)
					.items(List.of(OrderItemResponse.builder()
							.pizzaName("Margherita").quantity(2).build()))
					.build();

			when(orderService.getOrderByCode(orderCode)).thenReturn(response);

			// When/Then
			mockMvc.perform(get("/api/orders/" + orderCode))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.orderCode", is(orderCode)))
					.andExpect(jsonPath("$.customerName", is("Luigi Verdi")))
					.andExpect(jsonPath("$.status", is("IN_PROGRESS")))
					.andExpect(jsonPath("$.createdAt").exists())
					.andExpect(jsonPath("$.items").isArray())
					.andExpect(jsonPath("$.items", hasSize(1)))
					.andExpect(jsonPath("$.items[0].pizzaName", is("Margherita")))
					.andExpect(jsonPath("$.items[0].quantity", is(2)));
		}

		@Test
		@DisplayName("dovrebbe restituire 404 se l'ordine non esiste")
		void shouldReturn404WhenOrderNotFound() throws Exception {
			// Given
			String nonExistentCode = UUID.randomUUID().toString();
			when(orderService.getOrderByCode(nonExistentCode))
					.thenThrow(new OrderNotFoundException(nonExistentCode));

			// When/Then
			mockMvc.perform(get("/api/orders/" + nonExistentCode))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.status").value(404));
		}
	}

	@Nested
	@DisplayName("GET /api/orders/{orderCode}/status")
	class GetOrderStatusTests {

		@Test
		@DisplayName("dovrebbe restituire lo stato dell'ordine")
		void shouldReturnOrderStatus() throws Exception {
			// Given
			String orderCode = UUID.randomUUID().toString();
			OrderStatusResponse response = OrderStatusResponse.builder()
					.status(OrderStatus.IN_PROGRESS)
					.build();

			when(orderService.getOrderStatusByCode(orderCode)).thenReturn(response);

			// When/Then
			mockMvc.perform(get("/api/orders/" + orderCode + "/status"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.status", is("IN_PROGRESS")));
		}

		@Test
		@DisplayName("dovrebbe restituire 404 se l'ordine non esiste")
		void shouldReturn404WhenOrderNotFoundForStatus() throws Exception {
			// Given
			String nonExistentCode = UUID.randomUUID().toString();
			when(orderService.getOrderStatusByCode(nonExistentCode))
					.thenThrow(new OrderNotFoundException(nonExistentCode));

			// When/Then
			mockMvc.perform(get("/api/orders/" + nonExistentCode + "/status"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.status").value(404));
		}
	}

	@Nested
	@DisplayName("GET /api/orders/queue")
	class GetQueueTests {

		@Test
		@DisplayName("dovrebbe restituire la coda degli ordini")
		void shouldReturnOrderQueue() throws Exception {
			// Given
			String orderCode1 = UUID.randomUUID().toString();
			String orderCode2 = UUID.randomUUID().toString();
			LocalDateTime createdAt1 = LocalDateTime.of(2026, 2, 3, 10, 0, 0);
			LocalDateTime createdAt2 = LocalDateTime.of(2026, 2, 3, 10, 30, 0);
			List<OrderResponse> queue = List.of(
					OrderResponse.builder()
							.orderCode(orderCode1)
							.customerName("Primo")
							.status(OrderStatus.PENDING)
							.createdAt(createdAt1)
							.items(List.of(OrderItemResponse.builder()
									.pizzaName("Margherita").quantity(1).build()))
							.build(),
					OrderResponse.builder()
							.orderCode(orderCode2)
							.customerName("Secondo")
							.status(OrderStatus.PENDING)
							.createdAt(createdAt2)
							.items(List.of())
							.build()
			);

			when(orderService.getOrderQueue()).thenReturn(queue);

			// When/Then
			mockMvc.perform(get("/api/orders/queue"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(2)))
					.andExpect(jsonPath("$[0].orderCode", is(orderCode1)))
					.andExpect(jsonPath("$[1].orderCode", is(orderCode2)));
		}

		@Test
		@DisplayName("dovrebbe restituire una lista vuota quando la coda è vuota")
		void shouldReturnEmptyQueue() throws Exception {
			// Given
			when(orderService.getOrderQueue()).thenReturn(Collections.emptyList());

			// When/Then
			mockMvc.perform(get("/api/orders/queue"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$", hasSize(0)));
		}
	}

	@Nested
	@DisplayName("PUT /api/orders/next")
	class TakeNextOrderTests {

		@Test
		@DisplayName("dovrebbe prendere il prossimo ordine in coda")
		void shouldTakeNextOrder() throws Exception {
			// Given
			String orderCode = UUID.randomUUID().toString();
			LocalDateTime createdAt = LocalDateTime.of(2026, 2, 3, 10, 30, 0);
			OrderResponse response = OrderResponse.builder()
					.orderCode(orderCode)
					.customerName("Cliente")
					.status(OrderStatus.IN_PROGRESS)
					.createdAt(createdAt)
					.items(List.of(OrderItemResponse.builder()
							.pizzaName("Quattro Formaggi").quantity(1).build()))
					.build();

			when(orderService.takeNextOrder()).thenReturn(response);

			// When/Then
			mockMvc.perform(post("/api/orders/next"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.orderCode", is(orderCode)))
					.andExpect(jsonPath("$.customerName", is("Cliente")))
					.andExpect(jsonPath("$.status", is("IN_PROGRESS")))
					.andExpect(jsonPath("$.createdAt").exists())
					.andExpect(jsonPath("$.items").isArray())
					.andExpect(jsonPath("$.items", hasSize(1)));
		}

		@Test
		@DisplayName("dovrebbe restituire 404 se la coda è vuota")
		void shouldReturn404WhenQueueIsEmpty() throws Exception {
			// Given
			when(orderService.takeNextOrder()).thenThrow(new NoOrdersInQueueException());

			// When/Then
			mockMvc.perform(post("/api/orders/next"))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("PUT /api/orders/{orderCode}/complete")
	class CompleteOrderTests {

		@Test
		@DisplayName("dovrebbe completare l'ordine")
		void shouldCompleteOrder() throws Exception {
			// Given
			String orderCode = UUID.randomUUID().toString();
			LocalDateTime createdAt = LocalDateTime.of(2026, 2, 3, 10, 30, 0);
			OrderResponse response = OrderResponse.builder()
					.orderCode(orderCode)
					.customerName("Cliente")
					.status(OrderStatus.COMPLETED)
					.createdAt(createdAt)
					.items(List.of(OrderItemResponse.builder()
							.pizzaName("Diavola").quantity(2).build()))
					.build();

			when(orderService.completeOrder(eq(orderCode))).thenReturn(response);

			// When/Then
			mockMvc.perform(put("/api/orders/" + orderCode + "/complete"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.orderCode", is(orderCode)))
					.andExpect(jsonPath("$.customerName", is("Cliente")))
					.andExpect(jsonPath("$.status", is("COMPLETED")))
					.andExpect(jsonPath("$.createdAt").exists())
					.andExpect(jsonPath("$.items").isArray());
		}

		@Test
		@DisplayName("dovrebbe restituire 409 se l'ordine non è in lavorazione")
		void shouldReturn409WhenOrderNotInProgress() throws Exception {
			// Given
			String pendingOrderCode = UUID.randomUUID().toString();
			when(orderService.completeOrder(eq(pendingOrderCode)))
					.thenThrow(new InvalidOrderStateException("L'ordine deve essere IN_PROGRESS"));

			// When/Then
			mockMvc.perform(put("/api/orders/" + pendingOrderCode + "/complete"))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.status").value(409))
					.andExpect(jsonPath("$.message").exists());
		}

		@Test
		@DisplayName("dovrebbe restituire 404 se l'ordine non esiste")
		void shouldReturn404WhenOrderNotFoundForComplete() throws Exception {
			// Given
			String nonExistentCode = UUID.randomUUID().toString();
			when(orderService.completeOrder(eq(nonExistentCode)))
					.thenThrow(new OrderNotFoundException(nonExistentCode));

			// When/Then
			mockMvc.perform(put("/api/orders/" + nonExistentCode + "/complete"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.status").value(404))
					.andExpect(jsonPath("$.message").exists());
		}
	}
}
