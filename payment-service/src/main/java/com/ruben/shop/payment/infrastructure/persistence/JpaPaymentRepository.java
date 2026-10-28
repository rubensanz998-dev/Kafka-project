package com.ruben.shop.payment.infrastructure.persistence;

import com.ruben.shop.payment.domain.Payment;
import com.ruben.shop.payment.domain.PaymentRepository;
import org.springframework.stereotype.Repository;

/** Adaptador de salida. Un pago es inmutable una vez registrado, así que solo se inserta. */
@Repository
class JpaPaymentRepository implements PaymentRepository {

    private final SpringDataPayments jpa;

    JpaPaymentRepository(SpringDataPayments jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean exists(String orderId) {
        return jpa.existsById(orderId);
    }

    @Override
    public void save(Payment payment) {
        jpa.save(new PaymentJpaEntity(payment));
    }
}
