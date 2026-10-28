package com.ruben.shop.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Agregado: el pago de un pedido. La política de autorización (límite por pedido) es regla de dominio. */
public class Payment {

    public enum Status { COMPLETED, FAILED }

    public static final BigDecimal LIMIT = new BigDecimal("1000");

    private final String orderId;
    private final String customerId;
    private final BigDecimal amount;
    private final Status status;
    private final String reason;
    private final Instant createdAt;

    private Payment(String orderId, String customerId, BigDecimal amount, Status status, String reason,
                    Instant createdAt) {
        this.orderId = Objects.requireNonNull(orderId);
        this.customerId = Objects.requireNonNull(customerId);
        this.amount = Objects.requireNonNull(amount);
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    /** Decide si el pago se autoriza: por encima del límite se rechaza (dispara la compensación de la saga). */
    public static Payment authorize(String orderId, String customerId, BigDecimal amount) {
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("El importe debe ser positivo");
        }
        return amount.compareTo(LIMIT) > 0
                ? new Payment(orderId, customerId, amount, Status.FAILED, "Importe supera el límite de " + LIMIT,
                Instant.now())
                : new Payment(orderId, customerId, amount, Status.COMPLETED, null, Instant.now());
    }

    /** Pago que no se pudo cobrar por un fallo técnico persistente: la saga lo trata como un rechazo. */
    public static Payment unavailable(String orderId, String customerId, BigDecimal amount, String reason) {
        return new Payment(orderId, customerId, amount, Status.FAILED, reason, Instant.now());
    }

    public static Payment restore(String orderId, String customerId, BigDecimal amount, Status status,
                                  String reason, Instant createdAt) {
        return new Payment(orderId, customerId, amount, status, reason, createdAt);
    }

    public boolean isCompleted() {
        return status == Status.COMPLETED;
    }

    public String orderId() {
        return orderId;
    }

    public String customerId() {
        return customerId;
    }

    public BigDecimal amount() {
        return amount;
    }

    public Status status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
