package com.ruben.shop.order.domain;

/**
 * Eventos de dominio: hechos que ya ocurrieron dentro del agregado.
 * La infraestructura los traduce a eventos de integración (los de Kafka) en un único sitio.
 */
public sealed interface OrderEvent {

    OrderId orderId();

    record OrderPlaced(OrderId orderId, String customerId, String productId, int quantity, Money amount)
            implements OrderEvent {
    }

    record OrderConfirmed(OrderId orderId, String customerId) implements OrderEvent {
    }

    record OrderCancelled(OrderId orderId, String customerId, String reason) implements OrderEvent {
    }
}
