package com.awesomepizza.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.awesomepizza.api.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit test per GlobalExceptionHandler che testa direttamente i metodi handler
 * senza bisogno di MockMvc o contesto Spring completo.
 */
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerUnitTest {

	private GlobalExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
	}

	@Nested
	@DisplayName("handleIllegalState")
	class HandleIllegalStateTests {

		@Test
		@DisplayName("dovrebbe restituire 400 Bad Request con messaggio dell'eccezione")
		void shouldReturn400WithExceptionMessage() {
			// Given
			String errorMessage = "Operazione non consentita in questo stato";
			IllegalStateException ex = new IllegalStateException(errorMessage);

			// When
			ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getStatus()).isEqualTo(400);
			assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
		}

		@Test
		@DisplayName("dovrebbe gestire messaggio null")
		void shouldHandleNullMessage() {
			// Given
			IllegalStateException ex = new IllegalStateException((String) null);

			// When
			ResponseEntity<ErrorResponse> response = handler.handleIllegalState(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getStatus()).isEqualTo(400);
			assertThat(response.getBody().getMessage()).isNull();
		}
	}

	@Nested
	@DisplayName("handleGenericException")
	class HandleGenericExceptionTests {

		@Test
		@DisplayName("dovrebbe restituire 500 Internal Server Error con messaggio generico")
		void shouldReturn500WithGenericMessage() {
			// Given
			Exception ex = new Exception("Database connection failed");

			// When
			ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getStatus()).isEqualTo(500);
			assertThat(response.getBody().getMessage()).isEqualTo("Errore interno del server");
		}

		@Test
		@DisplayName("dovrebbe mascherare dettagli interni dell'errore")
		void shouldMaskInternalErrorDetails() {
			// Given - eccezione con dettagli sensibili
			Exception ex = new Exception("Connection to DB at 192.168.1.1:5432 failed with password auth");

			// When
			ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

			// Then - il messaggio non dovrebbe contenere dettagli sensibili
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getMessage()).doesNotContain("192.168.1.1");
			assertThat(response.getBody().getMessage()).doesNotContain("password");
			assertThat(response.getBody().getMessage()).isEqualTo("Errore interno del server");
		}

		@Test
		@DisplayName("dovrebbe gestire RuntimeException come eccezione generica")
		void shouldHandleRuntimeExceptionAsGeneric() {
			// Given
			RuntimeException ex = new RuntimeException("Unexpected error");

			// When
			ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getStatus()).isEqualTo(500);
		}

		@Test
		@DisplayName("dovrebbe gestire NullPointerException come eccezione generica")
		void shouldHandleNullPointerExceptionAsGeneric() {
			// Given
			NullPointerException ex = new NullPointerException("Cannot invoke method on null");

			// When
			ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getStatus()).isEqualTo(500);
		}
	}

	@Nested
	@DisplayName("handleOrderNotFound")
	class HandleOrderNotFoundTests {

		@Test
		@DisplayName("dovrebbe restituire 404 con messaggio dell'eccezione")
		void shouldReturn404WithExceptionMessage() {
			// Given
			String orderCode = "550e8400-e29b-41d4-a716-446655440000";
			OrderNotFoundException ex = new OrderNotFoundException(orderCode);

			// When
			ResponseEntity<ErrorResponse> response = handler.handleOrderNotFound(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getStatus()).isEqualTo(404);
			assertThat(response.getBody().getMessage()).contains(orderCode);
		}
	}

	@Nested
	@DisplayName("handleNoOrdersInQueue")
	class HandleNoOrdersInQueueTests {

		@Test
		@DisplayName("dovrebbe restituire 204 No Content")
		void shouldReturn204NoContent() {
			// Given
			NoOrdersInQueueException ex = new NoOrdersInQueueException();

			// When
			ResponseEntity<ErrorResponse> response = handler.handleNoOrdersInQueue(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		}
	}

	@Nested
	@DisplayName("handleOrderAlreadyInProgress")
	class HandleOrderAlreadyInProgressTests {

		@Test
		@DisplayName("dovrebbe restituire 409 Conflict")
		void shouldReturn409Conflict() {
			// Given
			OrderAlreadyInProgressException ex = new OrderAlreadyInProgressException();

			// When
			ResponseEntity<ErrorResponse> response = handler.handleOrderAlreadyInProgress(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getStatus()).isEqualTo(409);
		}
	}

	@Nested
	@DisplayName("handleInvalidOrderState")
	class HandleInvalidOrderStateTests {

		@Test
		@DisplayName("dovrebbe restituire 400 Bad Request con messaggio")
		void shouldReturn400WithMessage() {
			// Given
			String errorMessage = "L'ordine deve essere IN_PROGRESS per essere completato";
			InvalidOrderStateException ex = new InvalidOrderStateException(errorMessage);

			// When
			ResponseEntity<ErrorResponse> response = handler.handleInvalidOrderState(ex);

			// Then
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(response.getBody()).isNotNull();
			assertThat(response.getBody().getStatus()).isEqualTo(409);
			assertThat(response.getBody().getMessage()).isEqualTo(errorMessage);
		}
	}
}
