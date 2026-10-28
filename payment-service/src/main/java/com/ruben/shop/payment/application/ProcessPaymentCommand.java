package com.ruben.shop.payment.application;

import java.math.BigDecimal;

public record ProcessPaymentCommand(String orderId, String customerId, BigDecimal amount) {
}
