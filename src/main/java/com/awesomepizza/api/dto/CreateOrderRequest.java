package com.awesomepizza.api.dto;

import com.awesomepizza.api.dto.validator.annotation.NoDuplicatePizzaNames;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

	@NotBlank(message = "Il nome del cliente è obbligatorio")
	@Size(max = 255, message = "Il nome del cliente non può superare 255 caratteri")
	private String customerName;

	@NotEmpty(message = "L'ordine deve contenere almeno una pizza")
	@Valid
	@NoDuplicatePizzaNames
	private List<OrderItemRequest> items;
}
