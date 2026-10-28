package com.ruben.shop.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InventoryDomainTest {

    @Test
    void reserveDecrementsStockWhenAvailable() {
        var product = new Product("PHONE", 5);

        var reservation = product.reserve("o1", 2);

        assertThat(product.stock()).isEqualTo(3);
        assertThat(reservation.status()).isEqualTo(Reservation.Status.RESERVED);
    }

    @Test
    void reserveIsRejectedWithoutTouchingStockWhenNotEnough() {
        var product = new Product("PHONE", 5);

        var reservation = product.reserve("o1", 6);

        assertThat(product.stock()).isEqualTo(5);
        assertThat(reservation.isRejected()).isTrue();
        assertThat(reservation.reason()).isEqualTo("Stock insuficiente de PHONE (disponible: 5)");
    }

    @Test
    void reservationCanBeReleasedOrConfirmedOnlyOnce() {
        var released = new Product("PHONE", 5).reserve("o1", 1);
        assertThat(released.release()).isTrue();
        assertThat(released.release()).isFalse();
        assertThat(released.confirm()).isFalse();

        var confirmed = new Product("PHONE", 5).reserve("o2", 1);
        assertThat(confirmed.confirm()).isTrue();
        assertThat(confirmed.release()).as("un pedido pagado no devuelve stock").isFalse();
    }

    @Test
    void rejectedReservationNeverReleasesStock() {
        assertThat(Reservation.rejected("o1", "PHONE", 1, "x").release()).isFalse();
    }

    @Test
    void invariants() {
        assertThatThrownBy(() -> new Product("PHONE", -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Product("PHONE", 5).reserve("o1", 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
