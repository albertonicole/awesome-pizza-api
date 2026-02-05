package com.awesomepizza.api.exception;


/**
 * Eccezione lanciata quando si tenta una transizione di stato non valida su un ordine.
 */
public class InvalidOrderStateException extends RuntimeException {

	public InvalidOrderStateException(String message) {
		super(message);
	}
}
