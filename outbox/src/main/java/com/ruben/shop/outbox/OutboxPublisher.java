package com.ruben.shop.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Envía a Kafka los eventos pendientes de la outbox, en orden de inserción.
 * Si Kafka está caído, la transacción hace rollback y los eventos se reintentan en la siguiente pasada:
 * entrega at-least-once, por eso los consumidores son idempotentes.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, Object> kafka;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxEventRepository repository, KafkaTemplate<String, Object> kafka,
                           ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    // shortcut: polling cada 500 ms; con mucho volumen se cambia por CDC (Debezium leyendo el WAL de Postgres).
    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPending() throws Exception {
        for (OutboxEvent event : repository.findTop100BySentAtIsNullOrderByIdAsc()) {
            // Se reconstruye el objeto para que JsonSerializer añada la cabecera de tipo que esperan los consumidores.
            Object payload = objectMapper.readValue(event.getPayload(), Class.forName(event.getPayloadType()));
            // Envío síncrono, uno detrás de otro: mantiene el orden por clave dentro de la partición.
            kafka.send(event.getTopic(), event.getEventKey(), payload).get(10, TimeUnit.SECONDS);
            event.markSent();
            log.debug("Outbox {} publicado en {}", event.getId(), event.getTopic());
        }
    }
}
