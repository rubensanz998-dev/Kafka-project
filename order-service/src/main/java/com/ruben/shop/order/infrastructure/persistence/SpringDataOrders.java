package com.ruben.shop.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SpringDataOrders extends JpaRepository<OrderJpaEntity, String> {

    List<OrderJpaEntity> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
