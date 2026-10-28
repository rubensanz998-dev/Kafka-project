package com.ruben.shop.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPayments extends JpaRepository<PaymentJpaEntity, String> {
}
