package com.ruben.shop.order.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests del dominio puro: sin Spring, sin base de datos, milisegundos. */
class OrderTest {

    private static Order newOrder() {
        return Order.place("ana", "LAPTOP", 1, Money.of("900"));
    }

    @Test
    void placeCreatesPendingOrderAndRecordsOrderPlaced() {
        var order = newOrder();

        assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.pullEvents()).singleElement().isInstanceOf(OrderEvent.OrderPlaced.class);
        assertThat(order.pullEvents()).as("los eventos se entregan una sola vez").isEmpty();
    }

    @Test
    void canOnlyLeavePendingOnce() {
        var order = newOrder();
        order.pullEvents();

        assertThat(order.confirm()).isTrue();
        assertThat(order.cancel("tarde")).isFalse();
        assertThat(order.confirm()).isFalse();

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.pullEvents()).singleElement().isInstanceOf(OrderEvent.OrderConfirmed.class);
    }

    @Test
    void cancelKeepsReason() {
        var order = newOrder();

        order.cancel("sin stock");

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.reason()).isEqualTo("sin stock");
    }

    @Test
    void rejectsInvalidOrders() {
        assertThatThrownBy(() -> Order.place("ana", "LAPTOP", 0, Money.of("10")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Order.place(" ", "LAPTOP", 1, Money.of("10")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Money.of("0")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restoredOrdersDoNotEmitEvents() {
        var order = Order.restore(OrderId.newId(), "ana", "LAPTOP", 1, Money.of("10"), OrderStatus.PENDING,
                null, java.time.Instant.now(), java.time.Instant.now());

        assertThat(order.pullEvents()).isEmpty();
    }
}
