package com.ruben.shop.order.infrastructure.persistence;

import com.ruben.shop.order.domain.Order;
import com.ruben.shop.order.domain.OrderId;
import com.ruben.shop.order.domain.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Adaptador de salida: implementa el puerto del dominio con Spring Data JPA. */
@Repository
class JpaOrderRepository implements OrderRepository {

    private final SpringDataOrders jpa;

    JpaOrderRepository(SpringDataOrders jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Order order) {
        // Si ya se cargó en esta transacción, findById devuelve la misma entidad gestionada (con su @Version).
        var entity = jpa.findById(order.id().value()).orElseGet(OrderJpaEntity::new);
        entity.copyFrom(order);
        jpa.save(entity);
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpa.findById(id.value()).map(OrderJpaEntity::toDomain);
    }

    @Override
    public List<Order> findByCustomer(String customerId) {
        return jpa.findByCustomerIdOrderByCreatedAtDesc(customerId).stream().map(OrderJpaEntity::toDomain).toList();
    }
}
