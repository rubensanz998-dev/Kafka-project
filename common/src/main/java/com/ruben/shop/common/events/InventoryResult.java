package com.ruben.shop.common.events;

import java.math.BigDecimal;

/** Lleva customerId y amount para que payment-service no tenga que consultar a nadie. */
public record InventoryResult(String orderId, String customerId, BigDecimal amount, Status status, String reason) {
    public enum Status { RESERVED, REJECTED }
}
