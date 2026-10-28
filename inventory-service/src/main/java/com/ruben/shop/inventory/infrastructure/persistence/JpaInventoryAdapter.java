package com.ruben.shop.inventory.infrastructure.persistence;

import com.ruben.shop.inventory.domain.Product;
import com.ruben.shop.inventory.domain.ProductRepository;
import com.ruben.shop.inventory.domain.Reservation;
import com.ruben.shop.inventory.domain.ReservationRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adaptador de salida: implementa los dos puertos de repositorio del dominio con Spring Data JPA. */
@Repository
class JpaInventoryAdapter implements ProductRepository, ReservationRepository {

    private final SpringDataProducts products;
    private final SpringDataReservations reservations;

    JpaInventoryAdapter(SpringDataProducts products, SpringDataReservations reservations) {
        this.products = products;
        this.reservations = reservations;
    }

    @Override
    public Optional<Product> findById(String id) {
        return products.findById(id).map(ProductJpaEntity::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return products.findAll().stream().map(ProductJpaEntity::toDomain).toList();
    }

    @Override
    public void save(Product product) {
        var entity = products.findById(product.id()).orElseGet(ProductJpaEntity::new);
        entity.copyFrom(product);
        products.save(entity);
    }

    @Override
    public boolean exists(String orderId) {
        return reservations.existsById(orderId);
    }

    @Override
    public Optional<Reservation> findByOrderId(String orderId) {
        return reservations.findById(orderId).map(ReservationJpaEntity::toDomain);
    }

    @Override
    public void save(Reservation reservation) {
        var entity = reservations.findById(reservation.orderId()).orElseGet(ReservationJpaEntity::new);
        entity.copyFrom(reservation);
        reservations.save(entity);
    }
}
