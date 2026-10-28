package com.ruben.shop.order;

import java.math.BigDecimal;

public record Order(String id, String customerId, String productId, int quantity, BigDecimal amount,
                    Status status, String reason) {

    public enum Status { PENDING, CONFIRMED, CANCELLED }

    public Order withStatus(Status newStatus, String newReason) {
        return new Order(id, customerId, productId, quantity, amount, newStatus, newReason);
    }
}
