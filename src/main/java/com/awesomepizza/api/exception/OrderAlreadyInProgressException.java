package com.awesomepizza.api.exception;

public class OrderAlreadyInProgressException extends RuntimeException {

	public OrderAlreadyInProgressException() {
		super("È già presente un ordine in lavorazione. Completare l'ordine corrente prima di procedere.");
	}
}
