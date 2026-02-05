package com.awesomepizza.api.dto;

import com.awesomepizza.api.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusResponse {

	private OrderStatus status;

}
