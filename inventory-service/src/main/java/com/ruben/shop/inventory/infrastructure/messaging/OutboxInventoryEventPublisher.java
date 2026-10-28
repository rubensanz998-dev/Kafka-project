package com.ruben.shop.inventory.infrastructure.messaging;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.inventory.application.InventoryEventPublisher;
import com.ruben.shop.inventory.application.ReserveStockCommand;
import com.ruben.shop.inventory.domain.Reservation;
import com.ruben.shop.outbox.Outbox;
import org.springframework.stereotype.Component;

/** Adaptador de salida: traduce la reserva al evento de integración InventoryResult y lo guarda en la outbox. */
@Component
class OutboxInventoryEventPublisher implements InventoryEventPublisher {

    private final Outbox outbox;

    OutboxInventoryEventPublisher(Outbox outbox) {
        this.outbox = outbox;
    }

    @Override
    public void reservationDecided(Reservation reservation, ReserveStockCommand context) {
        var status = reservation.isRejected() ? InventoryResult.Status.REJECTED : InventoryResult.Status.RESERVED;
        outbox.save(Topics.INVENTORY_RESULT, reservation.orderId(), new InventoryResult(reservation.orderId(),
                context.customerId(), context.amount(), status, reservation.reason()));
    }
}
