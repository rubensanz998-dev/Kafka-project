package com.ruben.shop.order.infrastructure.persistence;

import com.ruben.shop.order.domain.Money;
import com.ruben.shop.order.domain.Order;
import com.ruben.shop.order.domain.OrderId;
import com.ruben.shop.order.domain.OrderStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

/** Modelo de persistencia, separado del modelo de dominio: el dominio no conoce JPA. */
@Entity
@Table(name = "orders")
class OrderJpaEntity {

    @Id
    private String id;
    private String customerId;
    private String productId;
    private int quantity;
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;
    /** Optimistic locking: si dos eventos modifican el mismo pedido a la vez, uno falla y Kafka lo reintenta. */
    @Version
    private Long version;

    void copyFrom(Order order) {
        id = order.id().value();
        customerId = order.customerId();
        productId = order.productId();
        quantity = order.quantity();
        amount = order.amount().value();
        status = order.status();
        reason = order.reason();
        createdAt = order.createdAt();
        updatedAt = order.updatedAt();
    }

    Order toDomain() {
        return Order.restore(new OrderId(id), customerId, productId, quantity, new Money(amount),
                status, reason, createdAt, updatedAt);
    }
}
