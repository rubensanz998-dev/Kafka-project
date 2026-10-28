package com.ruben.shop.order.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Value object: importe positivo con 2 decimales. Un Money inválido no se puede construir. */
public record Money(BigDecimal value) {

    public Money {
        Objects.requireNonNull(value, "amount");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("El importe debe ser positivo: " + value);
        }
        value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }
}
