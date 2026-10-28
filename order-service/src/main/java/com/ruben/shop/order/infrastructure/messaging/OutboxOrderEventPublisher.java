package com.ruben.shop.order.infrastructure.messaging;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.OrderCreated;
import com.ruben.shop.common.events.OrderStatusChanged;
import com.ruben.shop.order.application.OrderEventPublisher;
import com.ruben.shop.order.domain.OrderEvent;
import com.ruben.shop.order.domain.OrderStatus;
import com.ruben.shop.outbox.Outbox;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adaptador de salida: traduce eventos de dominio a eventos de integración (el contrato público en Kafka)
 * y los guarda en la outbox. El dominio puede cambiar sin romper a los consumidores.
 */
@Component
class OutboxOrderEventPublisher implements OrderEventPublisher {

    private final Outbox outbox;

    OutboxOrderEventPublisher(Outbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void publish(List<OrderEvent> events) {
        // orderId como clave: todos los eventos de un pedido van a la misma partición y se procesan en orden.
        for (OrderEvent event : events) {
            String key = event.orderId().value();
            switch (event) {
                case OrderEvent.OrderPlaced e -> outbox.save(Topics.ORDER_CREATED, key,
                        new OrderCreated(key, e.customerId(), e.productId(), e.quantity(), e.amount().value()));
                case OrderEvent.OrderConfirmed e -> outbox.save(Topics.ORDER_STATUS, key,
                        new OrderStatusChanged(key, e.customerId(), OrderStatus.CONFIRMED.name(), null));
                case OrderEvent.OrderCancelled e -> outbox.save(Topics.ORDER_STATUS, key,
                        new OrderStatusChanged(key, e.customerId(), OrderStatus.CANCELLED.name(), e.reason()));
            }
        }
    }
}
