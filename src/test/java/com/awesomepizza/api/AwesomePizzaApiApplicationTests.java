package com.awesomepizza.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("AwesomePizzaApiApplication Tests")
class AwesomePizzaApiApplicationTests {

	@Test
	@DisplayName("il contesto Spring dovrebbe caricarsi correttamente")
	void contextLoads() {
		// Test intenzionalmente vuoto: verifica che il contesto Spring si carichi
		// correttamente senza errori. Se la configurazione dell'applicazione è errata
		// (es. bean mancanti, configurazioni invalide), questo test fallirà.
	}

}
