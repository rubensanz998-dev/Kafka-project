package com.ruben.shop.order.domain;

import java.util.List;
import java.util.Optional;

/** Puerto de salida: el dominio define qué necesita; la infraestructura decide cómo (JPA, en este caso). */
public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(OrderId id);

    List<Order> findByCustomer(String customerId);
}
