package com.ruben.shop.inventory;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.common.events.OrderCreated;
import com.ruben.shop.common.events.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Reserva stock al crear un pedido y lo libera (compensación) si el pago falla. */
@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    // shortcut: estado en memoria con stock inicial fijo; cambiar por una base de datos para producción.
    private final Map<String, Integer> stock = new HashMap<>(Map.of("LAPTOP", 10, "PHONE", 5, "MOUSE", 100));
    private final Map<String, OrderCreated> reservations = new HashMap<>();
    // shortcut: el set de pedidos procesados crece sin límite; en producción, tabla con TTL o clave única en BD.
    private final Set<String> processedOrders = new HashSet<>();
    private final KafkaTemplate<String, Object> kafka;

    public InventoryService(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    @KafkaListener(topics = Topics.ORDER_CREATED)
    public synchronized void onOrderCreated(OrderCreated order) {
        // Kafka entrega at-least-once: sin esta comprobación un reintento reservaría stock dos veces.
        if (!processedOrders.add(order.orderId())) {
            log.warn("Pedido {} duplicado, ignorado", order.orderId());
            return;
        }
        int available = stock.getOrDefault(order.productId(), 0);
        if (available < order.quantity()) {
            String reason = "Stock insuficiente de " + order.productId() + " (disponible: " + available + ")";
            log.info("Pedido {} rechazado: {}", order.orderId(), reason);
            publish(order, InventoryResult.Status.REJECTED, reason);
            return;
        }
        stock.put(order.productId(), available - order.quantity());
        reservations.put(order.orderId(), order);
        log.info("Pedido {}: reservadas {} uds de {}", order.orderId(), order.quantity(), order.productId());
        publish(order, InventoryResult.Status.RESERVED, null);
    }

    @KafkaListener(topics = Topics.PAYMENT_RESULT)
    public synchronized void onPaymentResult(PaymentResult payment) {
        var reservation = reservations.remove(payment.orderId());
        if (reservation == null || payment.status() == PaymentResult.Status.COMPLETED) {
            return; // pago OK: la reserva pasa a ser definitiva
        }
        stock.merge(reservation.productId(), reservation.quantity(), Integer::sum);
        log.info("Compensación: pedido {} liberó {} uds de {}", payment.orderId(),
                reservation.quantity(), reservation.productId());
    }

    public synchronized Map<String, Integer> currentStock() {
        return Map.copyOf(stock);
    }

    private void publish(OrderCreated order, InventoryResult.Status status, String reason) {
        kafka.send(Topics.INVENTORY_RESULT, order.orderId(),
                new InventoryResult(order.orderId(), order.customerId(), order.amount(), status, reason));
    }
}
