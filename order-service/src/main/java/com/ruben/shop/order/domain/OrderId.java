package com.ruben.shop.order.domain;

import java.util.Objects;
import java.util.UUID;

/** Value object: identidad del agregado Order. */
public record OrderId(String value) {

    public OrderId {
        Objects.requireNonNull(value, "orderId");
    }

    public static OrderId newId() {
        return new OrderId(UUID.randomUUID().toString());
    }
}
