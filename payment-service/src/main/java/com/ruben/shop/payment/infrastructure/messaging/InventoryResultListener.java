package com.ruben.shop.payment.infrastructure.messaging;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.payment.application.PaymentService;
import com.ruben.shop.payment.application.ProcessPaymentCommand;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Adaptador de entrada: solo se cobra cuando el inventario ha reservado el stock. */
@Component
class InventoryResultListener {

    private final PaymentService payments;

    InventoryResultListener(PaymentService payments) {
        this.payments = payments;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESULT)
    void onInventoryResult(InventoryResult event) {
        if (event.status() == InventoryResult.Status.RESERVED) {
            payments.processPayment(new ProcessPaymentCommand(event.orderId(), event.customerId(), event.amount()));
        }
    }
}
