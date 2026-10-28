package com.ruben.shop.payment.domain;

/** Puerto de salida. */
public interface PaymentRepository {

    boolean exists(String orderId);

    void save(Payment payment);
}
