package com.ruben.shop.inventory.infrastructure.persistence;

import com.ruben.shop.inventory.domain.Product;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "product")
class ProductJpaEntity {

    @Id
    private String id;
    private int stock;
    /** Optimistic locking: dos reservas simultáneas del mismo producto no pueden pisarse el stock. */
    @Version
    private Long version;

    void copyFrom(Product product) {
        id = product.id();
        stock = product.stock();
    }

    Product toDomain() {
        return new Product(id, stock);
    }
}
