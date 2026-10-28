package com.ruben.shop.payment.application;

import com.ruben.shop.payment.domain.Payment;

/** Puerto de salida: comunicar el resultado del pago al resto de la saga. */
public interface PaymentEventPublisher {

    void paymentProcessed(Payment payment);
}
