package com.ruben.shop.common.events;

import java.math.BigDecimal;

public record OrderCreated(String orderId, String customerId, String productId, int quantity, BigDecimal amount) {
}
