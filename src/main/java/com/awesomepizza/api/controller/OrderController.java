package com.awesomepizza.api.controller;

import com.awesomepizza.api.dto.CreateOrderRequest;
import com.awesomepizza.api.dto.OrderResponse;
import com.awesomepizza.api.dto.OrderStatusResponse;
import com.awesomepizza.api.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	/**
	 * Crea un nuovo ordine.
	 * POST /api/orders
	 */
	@PostMapping
	public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
		OrderResponse response = orderService.createOrder(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/**
	 * Recupera un ordine tramite il suo codice.
	 * GET /api/orders/{orderCode}
	 */
	@GetMapping("/{orderCode}")
	public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderCode) {
		OrderResponse response = orderService.getOrderByCode(orderCode);
		return ResponseEntity.ok(response);
	}

	/**
	 * Recupera lo stato di un ordine tramite il suo codice.
	 * GET /api/orders/{orderCode}/status
	 */
	@GetMapping("/{orderCode}/status")
	public ResponseEntity<OrderStatusResponse> getOrderStatus(@PathVariable String orderCode) {
		OrderStatusResponse response = orderService.getOrderStatusByCode(orderCode);
		return ResponseEntity.ok(response);
	}

	/**
	 * Restituisce la coda degli ordini in attesa.
	 * GET /api/orders/queue
	 */
	@GetMapping("/queue")
	public ResponseEntity<List<OrderResponse>> getOrderQueue() {
		List<OrderResponse> queue = orderService.getOrderQueue();
		return ResponseEntity.ok(queue);
	}

	/**
	 * Prende in carico il prossimo ordine in coda.
	 * PUT /api/orders/next
	 */
	@PutMapping("/next")
	public ResponseEntity<OrderResponse> takeNextOrder() {
		OrderResponse response = orderService.takeNextOrder();
		return ResponseEntity.ok(response);
	}

	/**
	 * Segna un ordine come completato.
	 * PUT /api/orders/{orderCode}/complete
	 */
	@PutMapping("/{orderCode}/complete")
	public ResponseEntity<OrderResponse> completeOrder(@PathVariable String orderCode) {
		OrderResponse response = orderService.completeOrder(orderCode);
		return ResponseEntity.ok(response);
	}
}
