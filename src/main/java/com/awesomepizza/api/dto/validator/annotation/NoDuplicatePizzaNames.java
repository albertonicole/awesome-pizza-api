package com.awesomepizza.api.dto.validator.annotation;

import com.awesomepizza.api.dto.validator.OrderItemRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OrderItemRequestValidator.class)
public @interface NoDuplicatePizzaNames {

	String message() default "L'ordine contiene pizze duplicate";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
