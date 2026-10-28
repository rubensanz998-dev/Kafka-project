package com.ruben.shop.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataProducts extends JpaRepository<ProductJpaEntity, String> {
}
