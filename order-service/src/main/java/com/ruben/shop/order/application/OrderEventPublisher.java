package com.ruben.shop.order.application;

import com.ruben.shop.order.domain.OrderEvent;

import java.util.List;

/** Puerto de salida: publicar los eventos de dominio fuera del servicio. */
public interface OrderEventPublisher {

    /** Debe ejecutarse en la misma transacción que el guardado del agregado. */
    void publish(List<OrderEvent> events);
}
