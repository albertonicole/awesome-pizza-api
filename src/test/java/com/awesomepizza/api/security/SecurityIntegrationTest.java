package com.awesomepizza.api.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private static final String CREATE_ORDER_JSON = """
			{
				"customerName": "Mario Rossi",
				"items": [{"pizzaName": "Margherita", "quantity": 1}]
			}
			""";

	@Nested
	@DisplayName("Unauthenticated requests (401)")
	class UnauthenticatedTests {

		@Test
		@DisplayName("POST /api/orders senza credenziali dovrebbe restituire 401")
		void createOrderWithoutAuthShouldReturn401() throws Exception {
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(CREATE_ORDER_JSON))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401))
					.andExpect(jsonPath("$.message").exists());
		}

		@Test
		@DisplayName("GET /api/orders/{code} senza credenziali dovrebbe restituire 401")
		void getOrderWithoutAuthShouldReturn401() throws Exception {
			mockMvc.perform(get("/api/orders/some-code"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401));
		}

		@Test
		@DisplayName("GET /api/orders/{code}/status senza credenziali dovrebbe restituire 401")
		void getOrderStatusWithoutAuthShouldReturn401() throws Exception {
			mockMvc.perform(get("/api/orders/some-code/status"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401));
		}

		@Test
		@DisplayName("GET /api/orders/queue senza credenziali dovrebbe restituire 401")
		void getQueueWithoutAuthShouldReturn401() throws Exception {
			mockMvc.perform(get("/api/orders/queue"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401));
		}

		@Test
		@DisplayName("POST /api/orders/next senza credenziali dovrebbe restituire 401")
		void takeNextOrderWithoutAuthShouldReturn401() throws Exception {
			mockMvc.perform(post("/api/orders/next"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401));
		}
	}

	@Nested
	@DisplayName("Forbidden requests (403)")
	class ForbiddenTests {

		@Test
		@DisplayName("PIZZAIOLO non può creare ordini")
		@WithMockUser(roles = "PIZZAIOLO")
		void pizzaioloCannotCreateOrder() throws Exception {
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(CREATE_ORDER_JSON))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.status").value(403))
					.andExpect(jsonPath("$.message").exists());
		}

		@Test
		@DisplayName("CLIENTE non può vedere la coda")
		@WithMockUser(roles = "CLIENTE")
		void clienteCannotViewQueue() throws Exception {
			mockMvc.perform(get("/api/orders/queue"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.status").value(403));
		}

		@Test
		@DisplayName("CLIENTE non può prendere il prossimo ordine")
		@WithMockUser(roles = "CLIENTE")
		void clienteCannotTakeNextOrder() throws Exception {
			mockMvc.perform(post("/api/orders/next"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.status").value(403));
		}

		@Test
		@DisplayName("CLIENTE non può completare un ordine")
		@WithMockUser(roles = "CLIENTE")
		void clienteCannotCompleteOrder() throws Exception {
			mockMvc.perform(put("/api/orders/some-code/complete"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.status").value(403));
		}
	}

	@Nested
	@DisplayName("Authorized requests")
	class AuthorizedTests {

		@Test
		@DisplayName("CLIENTE può creare un ordine")
		@WithMockUser(roles = "CLIENTE")
		void clienteCanCreateOrder() throws Exception {
			mockMvc.perform(post("/api/orders")
							.contentType(MediaType.APPLICATION_JSON)
							.content(CREATE_ORDER_JSON))
					.andExpect(status().isCreated());
		}

		@Test
		@DisplayName("CLIENTE può visualizzare un ordine")
		@WithMockUser(roles = "CLIENTE")
		void clienteCanViewOrder() throws Exception {
			// Ordine non esiste ma riceviamo 404 (non 401/403)
			mockMvc.perform(get("/api/orders/non-existent-code"))
					.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("PIZZAIOLO può visualizzare la coda")
		@WithMockUser(roles = "PIZZAIOLO")
		void pizzaioloCanViewQueue() throws Exception {
			mockMvc.perform(get("/api/orders/queue"))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("PIZZAIOLO può visualizzare un ordine")
		@WithMockUser(roles = "PIZZAIOLO")
		void pizzaioloCanViewOrder() throws Exception {
			mockMvc.perform(get("/api/orders/non-existent-code"))
					.andExpect(status().isNotFound());
		}
	}

	@Nested
	@DisplayName("Public endpoints")
	class PublicEndpointTests {

		@Test
		@DisplayName("Swagger UI è accessibile senza autenticazione")
		void swaggerUiIsPublic() throws Exception {
			mockMvc.perform(get("/swagger-ui/index.html"))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("API docs è accessibile senza autenticazione")
		void apiDocsIsPublic() throws Exception {
			mockMvc.perform(get("/v3/api-docs"))
					.andExpect(status().isOk());
		}

		@Test
		@DisplayName("Actuator è accessibile senza autenticazione")
		void actuatorIsPublic() throws Exception {
			mockMvc.perform(get("/actuator/health"))
					.andExpect(status().isOk());
		}
	}

	@Nested
	@DisplayName("HTTP Basic authentication")
	class HttpBasicTests {

		@Test
		@DisplayName("credenziali valide del cliente permettono l'accesso")
		void validClienteCredentialsShouldAuthenticate() throws Exception {
			mockMvc.perform(post("/api/orders")
							.with(httpBasic("cliente", "cliente123"))
							.contentType(MediaType.APPLICATION_JSON)
							.content(CREATE_ORDER_JSON))
					.andExpect(status().isCreated());
		}

		@Test
		@DisplayName("credenziali errate dovrebbero restituire 401")
		void invalidCredentialsShouldReturn401() throws Exception {
			mockMvc.perform(post("/api/orders")
							.with(httpBasic("cliente", "wrong-password"))
							.contentType(MediaType.APPLICATION_JSON)
							.content(CREATE_ORDER_JSON))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.status").value(401));
		}
	}
}
