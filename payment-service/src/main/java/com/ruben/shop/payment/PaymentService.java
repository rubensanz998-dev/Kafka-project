package com.ruben.shop.payment;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.common.events.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simula una pasarela de pago. Reglas de la demo:
 * - importe mayor que 1000 → pago rechazado (dispara la compensación en inventory-service)
 * - customerId "boom" → excepción técnica (2 reintentos y el mensaje acaba en inventory.result-dlt)
 */
@Service
public class PaymentService {

    static final BigDecimal LIMIT = new BigDecimal("1000");
    static final String FAILING_CUSTOMER = "boom";

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // shortcut: idempotencia en memoria; en producción, clave única en BD o idempotency key de la pasarela.
    private final Set<String> processedOrders = ConcurrentHashMap.newKeySet();
    private final KafkaTemplate<String, Object> kafka;

    public PaymentService(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESULT)
    public void onInventoryResult(InventoryResult event) {
        if (event.status() != InventoryResult.Status.RESERVED) {
            return; // sin stock no hay nada que cobrar
        }
        if (FAILING_CUSTOMER.equals(event.customerId())) {
            // Se lanza antes de marcarlo como procesado para que los reintentos vuelvan a intentarlo.
            throw new IllegalStateException("Pasarela de pago caída (simulado) para pedido " + event.orderId());
        }
        if (!processedOrders.add(event.orderId())) {
            log.warn("Pedido {} ya cobrado, ignorado", event.orderId());
            return;
        }
        var result = event.amount().compareTo(LIMIT) > 0
                ? new PaymentResult(event.orderId(), PaymentResult.Status.FAILED, "Importe supera el límite de " + LIMIT)
                : new PaymentResult(event.orderId(), PaymentResult.Status.COMPLETED, null);
        log.info("Pago pedido {}: {}", event.orderId(), result.status());
        kafka.send(Topics.PAYMENT_RESULT, event.orderId(), result);
    }
}
