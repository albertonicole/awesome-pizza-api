package com.awesomepizza.api.dto.validator;

import com.awesomepizza.api.dto.OrderItemRequest;
import com.awesomepizza.api.dto.validator.annotation.NoDuplicatePizzaNames;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OrderItemRequestValidator implements ConstraintValidator<NoDuplicatePizzaNames, List<OrderItemRequest>> {

	@Override
	public boolean isValid(List<OrderItemRequest> items, ConstraintValidatorContext context) {
		if (items == null || items.isEmpty()) {
			return true;
		}

		Set<String> seen = new HashSet<>();
		for (OrderItemRequest item : items) {
			if (item.getPizzaName() != null) {
				String normalized = item.getPizzaName().trim().toLowerCase();
				if (!seen.add(normalized)) {
					return false;
				}
			}
		}
		return true;
	}
}
