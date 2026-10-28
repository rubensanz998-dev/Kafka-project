package com.ruben.shop.order.application;

import com.ruben.shop.order.domain.Money;

public record PlaceOrderCommand(String customerId, String productId, int quantity, Money amount) {
}
