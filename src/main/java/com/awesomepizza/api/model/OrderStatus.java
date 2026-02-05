package com.awesomepizza.api.model;

/**
 * Rappresenta lo stato di un ordine nel suo ciclo di vita.
 */
public enum OrderStatus {
	/**
	 * Ordine ricevuto, in attesa di essere preso in carico.
	 */
	PENDING,

	/**
	 * Ordine in preparazione dal pizzaiolo.
	 */
	IN_PROGRESS,

	/**
	 * Ordine completato, pizza pronta.
	 */
	COMPLETED
}
