package com.awesomepizza.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

	@NotBlank(message = "Il nome della pizza è obbligatorio")
	@Size(max = 100, message = "Il nome della pizza non può superare 100 caratteri")
	private String pizzaName;

	@Min(value = 1, message = "La quantità deve essere almeno 1")
	@Max(value = 100, message = "La quantità massima per pizza è 100")
	@Builder.Default
	private Integer quantity = 1;
}
