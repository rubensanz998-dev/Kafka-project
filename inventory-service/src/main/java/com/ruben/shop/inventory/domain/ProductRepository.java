package com.ruben.shop.inventory.domain;

import java.util.List;
import java.util.Optional;

/** Puerto de salida. */
public interface ProductRepository {

    Optional<Product> findById(String id);

    List<Product> findAll();

    void save(Product product);
}
