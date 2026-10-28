package com.ruben.shop.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.payment.application.PaymentService;
import com.ruben.shop.payment.application.ProcessPaymentCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adaptador de entrada del DLT: un cobro que agotó los reintentos se da por fallido para que la saga compense
 * (pedido CANCELLED y stock devuelto). El mensaje se queda en el DLT como registro de lo que pasó.
 */
@Component
class DeadLetterListener {

    static final String REASON = "Pasarela de pago no disponible";

    private static final Logger log = LoggerFactory.getLogger(DeadLetterListener.class);

    private final PaymentService payments;
    private final ObjectMapper objectMapper;

    DeadLetterListener(PaymentService payments, ObjectMapper objectMapper) {
        this.payments = payments;
        this.objectMapper = objectMapper;
    }

    // Bytes en crudo: en el DLT también acaban poison pills, y si fallaran al deserializar irían a un "-dlt-dlt".
    @KafkaListener(topics = Topics.INVENTORY_RESULT + Topics.DLT_SUFFIX,
            properties = "value.deserializer=org.apache.kafka.common.serialization.ByteArrayDeserializer")
    void onDeadLetter(byte[] value) {
        InventoryResult event;
        try {
            event = objectMapper.readValue(value, InventoryResult.class);
        } catch (IOException | IllegalArgumentException e) {
            log.error("Mensaje ilegible en {}, ignorado", Topics.INVENTORY_RESULT + Topics.DLT_SUFFIX);
            return;
        }
        if (event == null) { // el literal JSON "null" también es un mensaje ilegible
            return;
        }
        // El DLT es compartido por todos los consumidores de inventory.result: solo los RESERVED son cobros fallidos.
        if (event.status() == InventoryResult.Status.RESERVED) {
            payments.failPayment(new ProcessPaymentCommand(event.orderId(), event.customerId(), event.amount()),
                    REASON);
        }
    }
}
