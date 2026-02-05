package com.awesomepizza.api.dto;

import com.awesomepizza.api.model.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

	private String orderCode;
	private String customerName;
	private OrderStatus status;
	private LocalDateTime createdAt;
	private List<OrderItemResponse> items;

}
