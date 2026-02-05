package com.awesomepizza.api.exception;

public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException(String orderCode) {
		super("Ordine non trovato con codice: " + orderCode);
	}
}
