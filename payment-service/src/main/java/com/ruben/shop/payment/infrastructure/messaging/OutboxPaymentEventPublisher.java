package com.ruben.shop.payment.infrastructure.messaging;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.PaymentResult;
import com.ruben.shop.outbox.Outbox;
import com.ruben.shop.payment.application.PaymentEventPublisher;
import com.ruben.shop.payment.domain.Payment;
import org.springframework.stereotype.Component;

/** Adaptador de salida: Payment → evento de integración PaymentResult en la outbox. */
@Component
class OutboxPaymentEventPublisher implements PaymentEventPublisher {

    private final Outbox outbox;

    OutboxPaymentEventPublisher(Outbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void paymentProcessed(Payment payment) {
        var status = payment.isCompleted() ? PaymentResult.Status.COMPLETED : PaymentResult.Status.FAILED;
        outbox.save(Topics.PAYMENT_RESULT, payment.orderId(),
                new PaymentResult(payment.orderId(), status, payment.reason()));
    }
}
