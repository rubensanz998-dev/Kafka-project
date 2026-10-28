package com.ruben.shop.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataReservations extends JpaRepository<ReservationJpaEntity, String> {
}
