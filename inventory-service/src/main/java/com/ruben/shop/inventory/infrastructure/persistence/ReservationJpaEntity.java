package com.ruben.shop.inventory.infrastructure.persistence;

import com.ruben.shop.inventory.domain.Reservation;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "reservation")
class ReservationJpaEntity {

    @Id
    private String orderId;
    private String productId;
    private int quantity;
    @Enumerated(EnumType.STRING)
    private Reservation.Status status;
    private String reason;
    private Instant createdAt;
    @Version
    private Long version;

    void copyFrom(Reservation reservation) {
        orderId = reservation.orderId();
        productId = reservation.productId();
        quantity = reservation.quantity();
        status = reservation.status();
        reason = reservation.reason();
        createdAt = reservation.createdAt();
    }

    Reservation toDomain() {
        return Reservation.restore(orderId, productId, quantity, status, reason, createdAt);
    }
}
