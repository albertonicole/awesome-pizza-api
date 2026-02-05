package com.awesomepizza.api.service;

import com.awesomepizza.api.dto.CreateOrderRequest;
import com.awesomepizza.api.dto.OrderResponse;
import com.awesomepizza.api.dto.OrderStatusResponse;
import com.awesomepizza.api.exception.InvalidOrderStateException;
import com.awesomepizza.api.exception.NoOrdersInQueueException;
import com.awesomepizza.api.exception.OrderAlreadyInProgressException;
import com.awesomepizza.api.exception.OrderNotFoundException;
import com.awesomepizza.api.mapper.OrderMapper;
import com.awesomepizza.api.model.Order;
import com.awesomepizza.api.model.OrderItem;
import com.awesomepizza.api.model.OrderStatus;
import com.awesomepizza.api.repository.OrderRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;

	private static final int MAX_QUEUE_SIZE = 100;

	/**
	 * Crea un nuovo ordine a partire dalla richiesta del cliente.
	 */
	@Transactional
	public OrderResponse createOrder(CreateOrderRequest request) {
		Order order = Order.builder()
				.orderCode(generateOrderCode())
				.customerName(request.getCustomerName())
				.status(OrderStatus.PENDING)
				.build();

		request.getItems().forEach(itemRequest -> {
			OrderItem item = OrderItem.builder()
					.pizzaName(itemRequest.getPizzaName())
					.quantity(itemRequest.getQuantity())
					.build();
			order.addItem(item);
		});

		Order savedOrder = orderRepository.save(order);
		log.info("Ordine creato: {} per cliente: {}", savedOrder.getOrderCode(), savedOrder.getCustomerName());
		return orderMapper.toOrderResponse(savedOrder);
	}

	/**
	 * Recupera un ordine tramite il suo codice.
	 */
	@Transactional(readOnly = true)
	public OrderResponse getOrderByCode(String orderCode) {
		Order order = orderRepository.findByOrderCode(orderCode)
				.orElseThrow(() -> new OrderNotFoundException(orderCode));
		return orderMapper.toOrderResponse(order);
	}

	/**
	 * Recupera lo stato di un ordine tramite il suo codice.
	 */
	@Transactional(readOnly = true)
	public OrderStatusResponse getOrderStatusByCode(String orderCode) {
		Order order = orderRepository.findByOrderCode(orderCode)
				.orElseThrow(() -> new OrderNotFoundException(orderCode));
		return orderMapper.toOrderStatusResponse(order);
	}


	/**
	 * Restituisce la coda degli ordini in stato PENDING, ordinati per data creazione (FIFO).
	 * Limitata a MAX_QUEUE_SIZE ordini per evitare problemi di performance.
	 */
	@Transactional(readOnly = true)
	public List<OrderResponse> getOrderQueue() {
		List<Order> queue = orderRepository.findByStatusWithItemsOrderByCreatedAtAsc(
				OrderStatus.PENDING, PageRequest.of(0, MAX_QUEUE_SIZE));
		log.debug("Coda ordini richiesta, {} ordini in coda", queue.size());
		return queue.stream()
				.map(orderMapper::toOrderResponse)
				.toList();
	}

	/**
	 * Prende in carico il prossimo ordine in coda (FIFO).
	 * Cambia lo stato da PENDING a IN_PROGRESS.
	 * Può esserci un solo ordine IN_PROGRESS contemporaneamente.
	 * Usa il pattern double-check per prevenire race condition:
	 * 1. Acquisisce lock pessimistico sul primo ordine PENDING
	 * 2. Verifica che non esista già un ordine IN_PROGRESS
	 * 3. Solo allora procede con il cambio di stato
	 */
	@Transactional
	public OrderResponse takeNextOrder() {
		// STEP 1: Acquisisce lock pessimistico sul primo ordine PENDING (FIFO)
		// Questo serializza l'accesso: altri thread aspettano qui
		Order order = orderRepository.findFirstByStatusOrderByCreatedAtAsc(OrderStatus.PENDING)
				.orElseThrow(NoOrdersInQueueException::new);

		// STEP 2: Double-check - ora che abbiamo il lock, verifichiamo
		// Se un altro thread ha completato prima di noi, esiste già un ordine in IN_PROGRESS
		checkNoOrderInProgress();

		// STEP 3: Sicuri di essere l'unico, procediamo
		order.setStatus(OrderStatus.IN_PROGRESS);
		Order savedOrder = orderRepository.save(order);
		log.info("Ordine {} preso in carico ({} -> {})", savedOrder.getOrderCode(), OrderStatus.PENDING, OrderStatus.COMPLETED);
		return orderMapper.toOrderResponse(savedOrder);
	}

	private void checkNoOrderInProgress() {
		if (orderRepository.existsByStatus(OrderStatus.IN_PROGRESS)) {
			log.warn("Tentativo di prendere un ordine mentre un altro è già in lavorazione");
			throw new OrderAlreadyInProgressException();
		}
	}

	/**
	 * Segna un ordine come completato.
	 */
	@Transactional
	public OrderResponse completeOrder(String orderCode) {
		Order order = orderRepository.findByOrderCodeWithLock(orderCode)
				.orElseThrow(() -> new OrderNotFoundException(orderCode));

		if (!order.getStatus().equals(OrderStatus.IN_PROGRESS)) {
			log.warn("Tentativo di completare ordine {} con stato {}", orderCode, order.getStatus());
			throw new InvalidOrderStateException(
					"L'ordine deve essere " + OrderStatus.IN_PROGRESS + " per essere completato. Stato attuale: " + order.getStatus());
		}

		order.setStatus(OrderStatus.COMPLETED);
		Order savedOrder = orderRepository.save(order);
		log.info("Ordine {} completato ({} -> {})", savedOrder.getOrderCode(), OrderStatus.IN_PROGRESS, OrderStatus.COMPLETED);
		return orderMapper.toOrderResponse(savedOrder);
	}

	/**
	 * Genera un codice ordine univoco nel formato UUID standard.
	 * Esempio: 550e8400-e29b-41d4-a716-446655440000
	 */
	private String generateOrderCode() {
		return UUID.randomUUID().toString();
	}
}
