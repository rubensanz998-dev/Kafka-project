package com.ruben.shop.inventory.infrastructure.messaging;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.OrderCreated;
import com.ruben.shop.common.events.PaymentResult;
import com.ruben.shop.inventory.application.InventoryService;
import com.ruben.shop.inventory.application.ReserveStockCommand;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: eventos de Kafka → casos de uso del inventario. */
@Component
class InventoryEventsListener {

    private final InventoryService inventory;

    InventoryEventsListener(InventoryService inventory) {
        this.inventory = inventory;
    }

    @KafkaListener(topics = Topics.ORDER_CREATED)
    void onOrderCreated(OrderCreated event) {
        inventory.reserveStock(new ReserveStockCommand(event.orderId(), event.customerId(), event.productId(),
                event.quantity(), event.amount()));
    }

    @KafkaListener(topics = Topics.PAYMENT_RESULT)
    void onPaymentResult(PaymentResult event) {
        if (event.status() == PaymentResult.Status.COMPLETED) {
            inventory.confirmReservation(event.orderId());
        } else {
            inventory.releaseReservation(event.orderId());
        }
    }
}
