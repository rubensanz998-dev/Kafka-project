package com.ruben.shop.payment.application;

import java.math.BigDecimal;

/** Puerto de salida hacia la pasarela de pago externa (Stripe, Redsys...). */
public interface PaymentGateway {

    /** Lanza una excepción si la pasarela falla técnicamente: Kafka reintentará el mensaje. */
    void charge(String orderId, String customerId, BigDecimal amount);
}
