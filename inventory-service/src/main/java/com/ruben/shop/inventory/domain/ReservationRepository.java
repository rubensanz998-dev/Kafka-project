package com.ruben.shop.inventory.domain;

import java.util.Optional;

/** Puerto de salida. */
public interface ReservationRepository {

    boolean exists(String orderId);

    Optional<Reservation> findByOrderId(String orderId);

    void save(Reservation reservation);
}
