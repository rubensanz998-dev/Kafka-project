package com.ruben.shop.order.infrastructure.messaging;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.common.events.PaymentResult;
import com.ruben.shop.order.application.OrderService;
import com.ruben.shop.order.domain.OrderId;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: convierte los eventos de los otros servicios en llamadas a casos de uso. */
@Component
class SagaEventsListener {

    private final OrderService orders;

    SagaEventsListener(OrderService orders) {
        this.orders = orders;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESULT)
    void onInventoryResult(InventoryResult event) {
        // Si hay stock, el pedido sigue PENDING hasta que responda payment-service.
        if (event.status() == InventoryResult.Status.REJECTED) {
            orders.cancel(new OrderId(event.orderId()), event.reason());
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_RESULT)
    void onPaymentResult(PaymentResult event) {
        var id = new OrderId(event.orderId());
        if (event.status() == PaymentResult.Status.COMPLETED) {
            orders.confirm(id);
        } else {
            orders.cancel(id, event.reason());
        }
    }
}
