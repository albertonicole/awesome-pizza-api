package com.awesomepizza.api.validator;

import static org.assertj.core.api.Assertions.assertThat;

import com.awesomepizza.api.dto.OrderItemRequest;
import com.awesomepizza.api.dto.validator.OrderItemRequestValidator;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("OrderItemRequestValidator Tests")
class OrderItemRequestValidatorTest {

	private OrderItemRequestValidator validator;

	@BeforeEach
	void setUp() {
		validator = new OrderItemRequestValidator();
	}

	@Nested
	@DisplayName("Validazione pizze duplicate")
	class DuplicateValidationTests {

		@Test
		@DisplayName("dovrebbe accettare pizze con nomi diversi")
		void shouldReturnTrueForUniqueItems() {
			List<OrderItemRequest> items = List.of(
					OrderItemRequest.builder().pizzaName("Margherita").quantity(1).build(),
					OrderItemRequest.builder().pizzaName("Diavola").quantity(2).build(),
					OrderItemRequest.builder().pizzaName("Quattro Formaggi").quantity(1).build()
			);

			assertThat(validator.isValid(items, null)).isTrue();
		}

		@Test
		@DisplayName("dovrebbe rifiutare pizze con nomi duplicati")
		void shouldReturnFalseForDuplicateItems() {
			List<OrderItemRequest> items = List.of(
					OrderItemRequest.builder().pizzaName("Margherita").quantity(2).build(),
					OrderItemRequest.builder().pizzaName("Margherita").quantity(3).build()
			);

			assertThat(validator.isValid(items, null)).isFalse();
		}

		@Test
		@DisplayName("dovrebbe rifiutare duplicati case-insensitive")
		void shouldReturnFalseForDuplicateItemsCaseInsensitive() {
			List<OrderItemRequest> items = List.of(
					OrderItemRequest.builder().pizzaName("margherita").quantity(1).build(),
					OrderItemRequest.builder().pizzaName("MARGHERITA").quantity(2).build()
			);

			assertThat(validator.isValid(items, null)).isFalse();
		}

		@Test
		@DisplayName("dovrebbe rifiutare duplicati con spazi extra")
		void shouldReturnFalseForDuplicateItemsWithWhitespace() {
			List<OrderItemRequest> items = List.of(
					OrderItemRequest.builder().pizzaName(" Margherita").quantity(1).build(),
					OrderItemRequest.builder().pizzaName("Margherita ").quantity(2).build()
			);

			assertThat(validator.isValid(items, null)).isFalse();
		}
	}

	@Nested
	@DisplayName("Casi limite")
	class EdgeCaseTests {

		@Test
		@DisplayName("dovrebbe accettare lista null")
		void shouldReturnTrueForNullList() {
			assertThat(validator.isValid(null, null)).isTrue();
		}

		@Test
		@DisplayName("dovrebbe accettare lista vuota")
		void shouldReturnTrueForEmptyList() {
			assertThat(validator.isValid(Collections.emptyList(), null)).isTrue();
		}

		@Test
		@DisplayName("dovrebbe accettare un singolo item")
		void shouldReturnTrueForSingleItem() {
			List<OrderItemRequest> items = List.of(
					OrderItemRequest.builder().pizzaName("Margherita").quantity(1).build()
			);

			assertThat(validator.isValid(items, null)).isTrue();
		}

		@Test
		@DisplayName("dovrebbe ignorare item con pizzaName null")
		void shouldIgnoreNullPizzaNames() {
			List<OrderItemRequest> items = List.of(
					OrderItemRequest.builder().pizzaName(null).quantity(1).build(),
					OrderItemRequest.builder().pizzaName("Margherita").quantity(2).build()
			);

			assertThat(validator.isValid(items, null)).isTrue();
		}
	}
}
