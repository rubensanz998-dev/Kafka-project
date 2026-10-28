package com.ruben.shop.payment.application;

import com.ruben.shop.payment.domain.Payment;
import com.ruben.shop.payment.domain.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Caso de uso: cobrar un pedido con stock reservado. */
@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final PaymentGateway gateway;
    private final PaymentEventPublisher events;

    public PaymentService(PaymentRepository payments, PaymentGateway gateway, PaymentEventPublisher events) {
        this.payments = payments;
        this.gateway = gateway;
        this.events = events;
    }

    public void processPayment(ProcessPaymentCommand cmd) {
        // La PK de payment garantiza además que un pedido no se cobra dos veces aunque lleguen copias a la vez.
        if (payments.exists(cmd.orderId())) {
            log.warn("Pedido {} ya cobrado, ignorado", cmd.orderId());
            return;
        }
        var payment = Payment.authorize(cmd.orderId(), cmd.customerId(), cmd.amount());
        if (payment.isCompleted()) {
            gateway.charge(cmd.orderId(), cmd.customerId(), cmd.amount());
        }
        payments.save(payment);
        log.info("Pago pedido {}: {}", cmd.orderId(), payment.status());
        events.paymentProcessed(payment);
    }

    /**
     * Compensación tras agotar los reintentos: registra el pago como fallido para que la saga cancele el pedido
     * y libere el stock en vez de dejarlo PENDING. Idempotente igual que processPayment.
     */
    // shortcut: con una pasarela real, antes de rendirse hay que consultar el cobro por orderId (clave de
    // idempotencia): si el cargo entró pero falló el commit, esto cancelaría un pedido ya cobrado.
    public void failPayment(ProcessPaymentCommand cmd, String reason) {
        if (payments.exists(cmd.orderId())) {
            log.warn("Pedido {} ya tenía pago, DLT ignorado", cmd.orderId());
            return;
        }
        var payment = Payment.unavailable(cmd.orderId(), cmd.customerId(), cmd.amount(), reason);
        payments.save(payment);
        log.warn("Pago pedido {}: FAILED ({})", cmd.orderId(), reason);
        events.paymentProcessed(payment);
    }
}
