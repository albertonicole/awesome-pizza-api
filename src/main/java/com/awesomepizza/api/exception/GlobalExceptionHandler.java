package com.awesomepizza.api.exception;

import com.awesomepizza.api.dto.ErrorResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(OrderNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
		log.warn("Ordine non trovato: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.builder()
						.message(ex.getMessage())
						.status(HttpStatus.NOT_FOUND.value())
						.build());
	}

	@ExceptionHandler(NoOrdersInQueueException.class)
	public ResponseEntity<ErrorResponse> handleNoOrdersInQueue(NoOrdersInQueueException ex) {
		log.info("Coda ordini vuota: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ErrorResponse.builder()
						.message("Coda ordini vuota")
						.status(HttpStatus.NOT_FOUND.value())
						.build());
	}

	@ExceptionHandler(OrderAlreadyInProgressException.class)
	public ResponseEntity<ErrorResponse> handleOrderAlreadyInProgress(OrderAlreadyInProgressException ex) {
		log.warn("Ordine già in lavorazione: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.builder()
						.message(ex.getMessage())
						.status(HttpStatus.CONFLICT.value())
						.build());
	}

	@ExceptionHandler(InvalidOrderStateException.class)
	public ResponseEntity<ErrorResponse> handleInvalidOrderState(InvalidOrderStateException ex) {
		log.warn("Stato ordine non valido: {}", ex.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ErrorResponse.builder()
						.message(ex.getMessage())
						.status(HttpStatus.CONFLICT.value())
						.build());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining(", "));
		log.warn("Errore di validazione: {}", message);
		return ResponseEntity.badRequest()
				.body(ErrorResponse.builder()
						.message(message)
						.status(HttpStatus.BAD_REQUEST.value())
						.build());
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
		log.warn("Illegal state: {}", ex.getMessage());
		return ResponseEntity.badRequest()
				.body(ErrorResponse.builder()
						.message(ex.getMessage())
						.status(HttpStatus.BAD_REQUEST.value())
						.build());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		log.warn("Richiesta con body non leggibile: {}", ex.getMessage());
		return ResponseEntity.badRequest()
				.body(ErrorResponse.builder()
						.message("Formato richiesta non valido")
						.status(HttpStatus.BAD_REQUEST.value())
						.build());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
		log.warn("Content-Type non supportato: {}", ex.getContentType());
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.body(ErrorResponse.builder()
						.message("Content-Type non supportato. Usare application/json")
						.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
						.build());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		log.error("Errore interno non gestito", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ErrorResponse.builder()
						.message("Errore interno del server")
						.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
						.build());
	}
}
