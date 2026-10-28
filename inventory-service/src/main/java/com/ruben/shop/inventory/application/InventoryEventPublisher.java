package com.ruben.shop.inventory.application;

import com.ruben.shop.inventory.domain.Reservation;

/** Puerto de salida: comunicar el resultado de una reserva al resto de la saga. */
public interface InventoryEventPublisher {

    void reservationDecided(Reservation reservation, ReserveStockCommand context);
}
