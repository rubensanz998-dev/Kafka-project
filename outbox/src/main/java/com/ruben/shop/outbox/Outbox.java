package com.ruben.shop.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional Outbox: en vez de publicar en Kafka directamente, el evento se guarda en la tabla outbox_event
 * en la MISMA transacción que el cambio de negocio. O se guardan los dos o ninguno: nunca hay un pedido
 * sin su evento ni un evento sin su pedido. {@link OutboxPublisher} los envía a Kafka después.
 */
@Component
public class Outbox {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public Outbox(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /** MANDATORY: falla si se llama fuera de una transacción, que es justo el error que el patrón quiere evitar. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(String topic, String key, Object event) {
        try {
            repository.save(new OutboxEvent(topic, key, event.getClass().getName(),
                    objectMapper.writeValueAsString(event)));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("No se puede serializar el evento " + event, e);
        }
    }
}
