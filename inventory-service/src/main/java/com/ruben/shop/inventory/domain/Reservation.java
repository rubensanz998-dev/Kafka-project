package com.ruben.shop.inventory.domain;

import java.time.Instant;

/**
 * Agregado: la reserva de stock de un pedido. Su ciclo de vida:
 * RESERVED → CONFIRMED (pago OK) o RELEASED (pago fallido). REJECTED si no había stock.
 */
public class Reservation {

    public enum Status { RESERVED, REJECTED, CONFIRMED, RELEASED }

    private final String orderId;
    private final String productId;
    private final int quantity;
    private final String reason;
    private final Instant createdAt;
    private Status status;

    private Reservation(String orderId, String productId, int quantity, Status status, String reason,
                        Instant createdAt) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    static Reservation reserved(String orderId, String productId, int quantity) {
        return new Reservation(orderId, productId, quantity, Status.RESERVED, null, Instant.now());
    }

    public static Reservation rejected(String orderId, String productId, int quantity, String reason) {
        return new Reservation(orderId, productId, quantity, Status.REJECTED, reason, Instant.now());
    }

    public static Reservation restore(String orderId, String productId, int quantity, Status status, String reason,
                                      Instant createdAt) {
        return new Reservation(orderId, productId, quantity, status, reason, createdAt);
    }

    /** @return false si no estaba RESERVED (evento duplicado): no cambia nada. */
    public boolean confirm() {
        if (status != Status.RESERVED) {
            return false;
        }
        status = Status.CONFIRMED;
        return true;
    }

    /** @return true si hay que devolver el stock; false si ya se había liberado o confirmado. */
    public boolean release() {
        if (status != Status.RESERVED) {
            return false;
        }
        status = Status.RELEASED;
        return true;
    }

    public boolean isRejected() {
        return status == Status.REJECTED;
    }

    public String orderId() {
        return orderId;
    }

    public String productId() {
        return productId;
    }

    public int quantity() {
        return quantity;
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
