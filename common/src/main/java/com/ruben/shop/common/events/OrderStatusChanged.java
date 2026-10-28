package com.ruben.shop.common.events;

public record OrderStatusChanged(String orderId, String customerId, String status, String reason) {
}
