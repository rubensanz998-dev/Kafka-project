package com.ruben.shop.common.events;

public record PaymentResult(String orderId, Status status, String reason) {
    public enum Status { COMPLETED, FAILED }
}
