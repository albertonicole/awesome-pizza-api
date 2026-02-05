package com.awesomepizza.api.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test per verificare che GlobalExceptionHandler restituisca correttamente
 * ErrorResponse con la struttura e i contenuti attesi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Nested
	@DisplayName("OrderNotFoundException")
	class OrderNotFoundExceptionTests {

		@Test
		@DisplayName("dovrebbe restituire 404 con ErrorResponse per ordine non trovato")
		void shouldReturn404WithErrorResponseForOrderNotFound() throws Exception {
			// When/Then
			MvcResult result = mockMvc.perform(get("/api/orders/NON-EXISTENT-CODE"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()))
					.andExpect(jsonPath("$.message").exists())
					.andReturn();

			// Verify response structure
			ErrorResponse response = objectMapper.readValue(
					result.getResponse().getContentAsString(), ErrorResponse.class);
			assertThat(response.getStatus()).isEqualTo(404);
			assertThat(response.getMessage()).contains("NON-EXISTENT-CODE");
		}
	}

	@Nested
	@DisplayName("NoOrdersInQueueException")
	class NoOrdersInQueueExceptionTests {

		@Test
		@DisplayName("dovrebbe restituire 204 No Content quando la coda è vuota")
		void shouldReturn204NoContentWhenQueueIsEmpty() throws Exception {
			// Given - assicurarsi che non ci siano ordini PENDING nella coda
			// Il test è @Transactional quindi il DB è pulito

			// When/Then
			mockMvc.perform(put("/api/orders/next"))
					.andExpect(status().isNoContent());
		}
	}

	@Nested
	@DisplayName("OrderAlreadyInProgressException")
	class OrderAlreadyInProgressExceptionTests {

		@Test
		@DisplayName("dovrebbe restituire 409 Conflict quando c'è già un ordine in lavorazione")
		void shouldReturn409WhenOrderAlreadyInProgress() throws Exception {
			// Given - crea due ordini
			String createOrderRequest = """
					{
						"customerName": "Mario Rossi",
						"items": [{"pizzaName": "Margherita", "quantity": 1}]
					}
					""";

			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(createOrderRequest))
					.andExpect(status().isCreated());

			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(createOrderRequest))
					.andExpect(status().isCreated());

			// Prende il primo ordine (IN_PROGRESS)
			mockMvc.perform(put("/api/orders/next"))
					.andExpect(status().isOk());

			// When - prova a prendere un altro ordine mentre uno è già in lavorazione
			MvcResult result = mockMvc.perform(put("/api/orders/next"))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
					.andExpect(jsonPath("$.message").exists())
					.andReturn();

			// Then
			ErrorResponse response = objectMapper.readValue(
					result.getResponse().getContentAsString(), ErrorResponse.class);
			assertThat(response.getStatus()).isEqualTo(409);
		}
	}

	@Nested
	@DisplayName("InvalidOrderStateException")
	class InvalidOrderStateExceptionTests {

		@Test
		@DisplayName("dovrebbe restituire 409 Bad Request per stato ordine non valido")
		void shouldReturn409ForInvalidOrderState() throws Exception {
			// Given - crea un ordine (stato PENDING)
			String createOrderRequest = """
					{
						"customerName": "Mario Rossi",
						"items": [{"pizzaName": "Margherita", "quantity": 1}]
					}
					""";

			MvcResult createResult = mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(createOrderRequest))
					.andExpect(status().isCreated())
					.andReturn();

			String responseJson = createResult.getResponse().getContentAsString();
			String orderCode = objectMapper.readTree(responseJson).get("orderCode").asText();

			// When - prova a completare un ordine PENDING (deve essere IN_PROGRESS)
			MvcResult result = mockMvc.perform(put("/api/orders/" + orderCode + "/complete"))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.status").value(HttpStatus.CONFLICT.value()))
					.andExpect(jsonPath("$.message").exists())
					.andReturn();

			// Then
			ErrorResponse response = objectMapper.readValue(
					result.getResponse().getContentAsString(), ErrorResponse.class);
			assertThat(response.getStatus()).isEqualTo(409);
			assertThat(response.getMessage()).containsIgnoringCase("IN_PROGRESS");
		}
	}

	@Nested
	@DisplayName("HttpMessageNotReadableException")
	class HttpMessageNotReadableExceptionTests {

		@Test
		@DisplayName("dovrebbe restituire 400 Bad Request per JSON malformato")
		void shouldReturn400ForMalformedJson() throws Exception {
			// Given - JSON malformato
			String malformedJson = "{ invalid json }";

			// When/Then
			MvcResult result = mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(malformedJson))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
					.andExpect(jsonPath("$.message").exists())
					.andReturn();

			// Then
			ErrorResponse response = objectMapper.readValue(
					result.getResponse().getContentAsString(), ErrorResponse.class);
			assertThat(response.getStatus()).isEqualTo(400);
		}
	}

	@Nested
	@DisplayName("HttpMediaTypeNotSupportedException")
	class HttpMediaTypeNotSupportedExceptionTests {

		@Test
		@DisplayName("dovrebbe restituire 415 Unsupported Media Type per Content-Type non supportato")
		void shouldReturn415ForUnsupportedMediaType() throws Exception {
			// Given - richiesta con Content-Type non supportato
			String validRequest = """
					{
						"customerName": "Mario Rossi",
						"items": [{"pizzaName": "Margherita", "quantity": 1}]
					}
					""";

			// When/Then
			MvcResult result = mockMvc.perform(post("/api/orders")
							.contentType(MediaType.TEXT_PLAIN)
							.content(validRequest))
					.andExpect(status().isUnsupportedMediaType())
					.andExpect(jsonPath("$.status").value(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()))
					.andExpect(jsonPath("$.message").exists())
					.andReturn();

			// Then
			ErrorResponse response = objectMapper.readValue(
					result.getResponse().getContentAsString(), ErrorResponse.class);
			assertThat(response.getStatus()).isEqualTo(415);
		}
	}

	@Nested
	@DisplayName("MethodArgumentNotValidException (Validation)")
	class ValidationExceptionTests {

		@Test
		@DisplayName("dovrebbe restituire 400 con ErrorResponse per validazione fallita - customerName mancante")
		void shouldReturn400WithErrorResponseForMissingCustomerName() throws Exception {
			// Given - richiesta senza customerName
			String invalidRequest = """
					{
						"items": [
							{"pizzaName": "Margherita", "quantity": 1}
						]
					}
					""";

			// When/Then
			MvcResult result = mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidRequest))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
					.andExpect(jsonPath("$.message").exists())
					.andReturn();

			// Verify response structure
			ErrorResponse response = objectMapper.readValue(
					result.getResponse().getContentAsString(), ErrorResponse.class);
			assertThat(response.getStatus()).isEqualTo(400);
			assertThat(response.getMessage()).containsIgnoringCase("customerName");
		}

		@Test
		@DisplayName("dovrebbe restituire 400 con ErrorResponse per validazione fallita - items vuoti")
		void shouldReturn400WithErrorResponseForEmptyItems() throws Exception {
			// Given - richiesta con items vuoti
			String invalidRequest = """
					{
						"customerName": "Mario Rossi",
						"items": []
					}
					""";

			// When/Then
			MvcResult result = mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidRequest))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
					.andExpect(jsonPath("$.message").exists())
					.andReturn();

			// Verify response structure
			ErrorResponse response = objectMapper.readValue(
					result.getResponse().getContentAsString(), ErrorResponse.class);
			assertThat(response.getStatus()).isEqualTo(400);
			assertThat(response.getMessage()).containsIgnoringCase("items");
		}

		@Test
		@DisplayName("dovrebbe restituire 400 con ErrorResponse per validazione fallita - item senza pizzaName")
		void shouldReturn400WithErrorResponseForMissingPizzaName() throws Exception {
			// Given - richiesta con item senza pizzaName
			String invalidRequest = """
					{
						"customerName": "Mario Rossi",
						"items": [
							{"quantity": 1}
						]
					}
					""";

			// When/Then
			MvcResult result = mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(invalidRequest))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.value()))
					.andExpect(jsonPath("$.message").exists())
					.andReturn();

			// Verify response structure
			ErrorResponse response = objectMapper.readValue(
					result.getResponse().getContentAsString(), ErrorResponse.class);
			assertThat(response.getStatus()).isEqualTo(400);
			assertThat(response.getMessage()).containsIgnoringCase("pizzaName");
		}
	}

	@Nested
	@DisplayName("ErrorResponse structure")
	class ErrorResponseStructureTests {

		@Test
		@DisplayName("ErrorResponse dovrebbe avere la struttura JSON corretta")
		void errorResponseShouldHaveCorrectJsonStructure() throws Exception {
			// When
			MvcResult result = mockMvc.perform(get("/api/orders/TEST-CODE"))
					.andExpect(status().isNotFound())
					.andReturn();

			// Then - verify JSON has exactly the expected fields
			String jsonResponse = result.getResponse().getContentAsString();
			assertThat(jsonResponse).contains("\"message\"");
			assertThat(jsonResponse).contains("\"status\"");

			// Verify it deserializes correctly
			ErrorResponse response = objectMapper.readValue(jsonResponse, ErrorResponse.class);
			assertThat(response).isNotNull();
			assertThat(response.getMessage()).isNotNull();
			assertThat(response.getStatus()).isGreaterThan(0);
		}
	}
}
