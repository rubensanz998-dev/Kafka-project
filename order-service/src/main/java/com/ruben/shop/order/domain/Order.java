package com.ruben.shop.order.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Agregado raíz. Toda regla sobre el ciclo de vida del pedido vive aquí, no en servicios ni controladores:
 * un pedido nace PENDING y solo puede salir de PENDING una vez (a CONFIRMED o CANCELLED).
 */
public class Order {

    private final OrderId id;
    private final String customerId;
    private final String productId;
    private final int quantity;
    private final Money amount;
    private final Instant createdAt;
    private OrderStatus status;
    private String reason;
    private Instant updatedAt;
    private final List<OrderEvent> pendingEvents = new ArrayList<>();

    private Order(OrderId id, String customerId, String productId, int quantity, Money amount,
                  OrderStatus status, String reason, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Factoría: la única forma de crear un pedido nuevo. Valida invariantes y registra OrderPlaced. */
    public static Order place(String customerId, String productId, int quantity, Money amount) {
        requireText(customerId, "customerId");
        requireText(productId, "productId");
        if (quantity < 1) {
            throw new IllegalArgumentException("La cantidad debe ser al menos 1");
        }
        var now = Instant.now();
        var order = new Order(OrderId.newId(), customerId, productId, quantity, amount,
                OrderStatus.PENDING, null, now, now);
        order.pendingEvents.add(new OrderEvent.OrderPlaced(order.id, customerId, productId, quantity, amount));
        return order;
    }

    /** Reconstruye un pedido ya existente (desde la base de datos). No genera eventos. */
    public static Order restore(OrderId id, String customerId, String productId, int quantity, Money amount,
                                OrderStatus status, String reason, Instant createdAt, Instant updatedAt) {
        return new Order(id, customerId, productId, quantity, amount, status, reason, createdAt, updatedAt);
    }

    /** @return false si el pedido ya estaba finalizado (evento duplicado o tardío): no cambia nada. */
    public boolean confirm() {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        finish(OrderStatus.CONFIRMED, null);
        pendingEvents.add(new OrderEvent.OrderConfirmed(id, customerId));
        return true;
    }

    /** @return false si el pedido ya estaba finalizado (evento duplicado o tardío): no cambia nada. */
    public boolean cancel(String reason) {
        if (status != OrderStatus.PENDING) {
            return false;
        }
        finish(OrderStatus.CANCELLED, reason);
        pendingEvents.add(new OrderEvent.OrderCancelled(id, customerId, reason));
        return true;
    }

    public boolean belongsTo(String customer) {
        return customerId.equals(customer);
    }

    /** Devuelve los eventos registrados y los vacía: cada evento se publica una sola vez. */
    public List<OrderEvent> pullEvents() {
        var events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    private void finish(OrderStatus newStatus, String newReason) {
        this.status = newStatus;
        this.reason = newReason;
        this.updatedAt = Instant.now();
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " es obligatorio");
        }
    }

    public OrderId id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public String productId() {
        return productId;
    }

    public int quantity() {
        return quantity;
    }

    public Money amount() {
        return amount;
    }

    public OrderStatus status() {
        return status;
    }

    public String reason() {
        return reason;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
