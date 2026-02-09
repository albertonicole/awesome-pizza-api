package com.awesomepizza.api.service;

import com.awesomepizza.api.dto.CreateOrderRequest;
import com.awesomepizza.api.dto.OrderResponse;
import com.awesomepizza.api.dto.OrderStatusResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {

	OrderResponse createOrder(CreateOrderRequest request);

	OrderResponse getOrderByCode(String orderCode);

	OrderStatusResponse getOrderStatusByCode(String orderCode);

	Page<OrderResponse> getOrderQueue(Pageable pageable);

	OrderResponse takeNextOrder();

	OrderResponse completeOrder(String orderCode);
}
