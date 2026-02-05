package com.awesomepizza.api.mapper;

import com.awesomepizza.api.dto.OrderItemResponse;
import com.awesomepizza.api.dto.OrderResponse;
import com.awesomepizza.api.dto.OrderStatusResponse;
import com.awesomepizza.api.model.Order;
import com.awesomepizza.api.model.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

	/**
	 * Converte un'entità Order in OrderResponse.
	 */
	OrderResponse toOrderResponse(Order order);

	/**
	 * Converte un'entità OrderItem in OrderItemResponse.
	 */
	OrderItemResponse toOrderItemResponse(OrderItem item);

	/**
	 * Converte un'entità Order in OrderStatusResponse.
	 */
	OrderStatusResponse toOrderStatusResponse(Order order);
}
