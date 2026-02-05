package com.awesomepizza.api.exception;

public class NoOrdersInQueueException extends RuntimeException {

	public NoOrdersInQueueException() {
		super("Nessun ordine in coda da processare");
	}
}
