package com.ruben.shop.payment.infrastructure.persistence;

import com.ruben.shop.payment.domain.Payment;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
class PaymentJpaEntity {

    @Id
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Payment.Status status;
    private String reason;
    private Instant createdAt;
    /** Con version null Spring Data hace INSERT (no merge): un duplicado choca con la PK en vez de sobrescribir. */
    @Version
    private Long version;

    protected PaymentJpaEntity() {
    }

    PaymentJpaEntity(Payment payment) {
        orderId = payment.orderId();
        customerId = payment.customerId();
        amount = payment.amount();
        status = payment.status();
        reason = payment.reason();
        createdAt = payment.createdAt();
    }

    Payment toDomain() {
        return Payment.restore(orderId, customerId, amount, status, reason, createdAt);
    }
}
