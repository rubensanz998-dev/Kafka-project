package com.ruben.shop.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * SELECT ... FOR UPDATE SKIP LOCKED: con varias instancias del servicio, cada una coge filas distintas
     * en vez de publicar el mismo evento dos veces o quedarse bloqueada esperando.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    List<OutboxEvent> findTop100BySentAtIsNullOrderByIdAsc();
}
