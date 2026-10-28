package com.ruben.shop.payment.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void authorizesUpToTheLimit() {
        var payment = Payment.authorize("o1", "c1", new BigDecimal("1000"));

        assertThat(payment.isCompleted()).isTrue();
        assertThat(payment.reason()).isNull();
    }

    @Test
    void rejectsAboveTheLimit() {
        var payment = Payment.authorize("o1", "c1", new BigDecimal("1000.01"));

        assertThat(payment.status()).isEqualTo(Payment.Status.FAILED);
        assertThat(payment.reason()).isEqualTo("Importe supera el límite de 1000");
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> Payment.authorize("o1", "c1", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
