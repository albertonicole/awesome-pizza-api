package com.awesomepizza.api.repository;

import com.awesomepizza.api.model.Order;
import com.awesomepizza.api.model.OrderStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	/**
	 * Trova un ordine tramite il suo codice univoco.
	 */
	Optional<Order> findByOrderCode(String orderCode);

	/**
	 * Trova un ordine tramite il suo codice univoco con pessimistic lock.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({
			@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
	})
	@Query("SELECT o FROM Order o WHERE o.orderCode = :orderCode")
	Optional<Order> findByOrderCodeWithLock(@Param("orderCode") String orderCode);

	/**
	 * Trova gli ID degli ordini con un determinato stato.
	 */
	@Query("SELECT o.id FROM Order o WHERE o.status = :status ORDER BY o.createdAt ASC")
	Page<Long> findIdsByStatusOrderByCreatedAtAsc(@Param("status") OrderStatus status, Pageable pageable);

	/**
	 * Trova gli ordini con i relativi items dato un elenco di ID.
	 */
	@Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id IN :ids ORDER BY o.createdAt ASC")
	List<Order> findByIdsWithItems(@Param("ids") List<Long> ids);

	/**
	 * Trova il primo ordine con lo stato specificato (FIFO) con pessimistic lock.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints({
			@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")
	})
	Optional<Order> findFirstByStatusOrderByCreatedAtAsc(OrderStatus status);

	/**
	 * Verifica se esiste almeno un ordine con lo stato specificato.
	 */
	boolean existsByStatus(OrderStatus status);

	/**
	 * Conta gli ordini con lo stato specificato.
	 */
	long countByStatus(OrderStatus status);
}
