package com.ruben.shop.payment.infrastructure.gateway;

import com.ruben.shop.payment.application.PaymentGateway;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Adaptador de salida simulado. Para la demo, el cliente "boom" simula una pasarela caída:
 * excepción → 2 reintentos → el mensaje acaba en inventory.result-dlt.
 */
// shortcut: pasarela falsa; sustituir por un adaptador real (Stripe, Redsys...) implementando PaymentGateway.
@Component
class FakePaymentGateway implements PaymentGateway {

    static final String FAILING_CUSTOMER = "boom";

    @Override
    public void charge(String orderId, String customerId, BigDecimal amount) {
        if (FAILING_CUSTOMER.equals(customerId)) {
            throw new IllegalStateException("Pasarela de pago caída (simulado) para pedido " + orderId);
        }
    }
}
