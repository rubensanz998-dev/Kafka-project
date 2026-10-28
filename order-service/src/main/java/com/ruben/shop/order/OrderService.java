package com.ruben.shop.order;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.common.events.OrderCreated;
import com.ruben.shop.common.events.OrderStatusChanged;
import com.ruben.shop.common.events.PaymentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Orquesta el ciclo de vida del pedido reaccionando a los eventos de inventario y pago. */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // shortcut: almacenamiento en memoria, se pierde al reiniciar; cambiar por Postgres + patrón Outbox para producción.
    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, Object> kafka;

    public OrderService(KafkaTemplate<String, Object> kafka) {
        this.kafka = kafka;
    }

    public Order create(String customerId, String productId, int quantity, BigDecimal amount) {
        var order = new Order(UUID.randomUUID().toString(), customerId, productId, quantity, amount,
                Order.Status.PENDING, null);
        orders.put(order.id(), order);
        // orderId como clave: todos los eventos de un pedido caen en la misma partición y se procesan en orden.
        kafka.send(Topics.ORDER_CREATED, order.id(),
                new OrderCreated(order.id(), customerId, productId, quantity, amount));
        log.info("Pedido {} creado (PENDING)", order.id());
        return order;
    }

    public Optional<Order> find(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public Collection<Order> findAll() {
        return orders.values();
    }

    @KafkaListener(topics = Topics.INVENTORY_RESULT)
    public void onInventoryResult(InventoryResult event) {
        // Si hay stock, el pedido sigue PENDING hasta que responda payment-service.
        if (event.status() == InventoryResult.Status.REJECTED) {
            transition(event.orderId(), Order.Status.CANCELLED, event.reason());
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_RESULT)
    public void onPaymentResult(PaymentResult event) {
        if (event.status() == PaymentResult.Status.COMPLETED) {
            transition(event.orderId(), Order.Status.CONFIRMED, null);
        } else {
            transition(event.orderId(), Order.Status.CANCELLED, event.reason());
        }
    }

    /** Solo se sale de PENDING una vez: los eventos duplicados (entrega at-least-once) se ignoran. */
    void transition(String orderId, Order.Status status, String reason) {
        var updated = new Order[1];
        orders.computeIfPresent(orderId, (id, current) -> {
            if (current.status() != Order.Status.PENDING) {
                return current;
            }
            updated[0] = current.withStatus(status, reason);
            return updated[0];
        });
        if (updated[0] == null) {
            log.warn("Evento ignorado para pedido {} (desconocido o ya finalizado)", orderId);
            return;
        }
        var order = updated[0];
        log.info("Pedido {} -> {}{}", orderId, status, reason == null ? "" : " (" + reason + ")");
        kafka.send(Topics.ORDER_STATUS, orderId,
                new OrderStatusChanged(orderId, order.customerId(), status.name(), reason));
    }
}
