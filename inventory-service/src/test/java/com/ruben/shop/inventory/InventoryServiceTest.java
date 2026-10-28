package com.ruben.shop.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.inventory.application.InventoryService;
import com.ruben.shop.inventory.application.ReserveStockCommand;
import com.ruben.shop.inventory.domain.Reservation;
import com.ruben.shop.inventory.domain.ReservationRepository;
import com.ruben.shop.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Casos de uso con sus adaptadores reales (JPA + outbox) sobre H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InventoryServiceTest {

    @Autowired
    InventoryService service;
    @Autowired
    ReservationRepository reservations;
    @Autowired
    OutboxEventRepository outbox;
    @Autowired
    ObjectMapper objectMapper;

    private void reserve(String orderId, String product, int quantity) {
        service.reserveStock(new ReserveStockCommand(orderId, "c1", product, quantity, new BigDecimal("300")));
    }

    private Reservation.Status reservationStatus(String orderId) {
        return reservations.findByOrderId(orderId).orElseThrow().status();
    }

    private InventoryResult lastPublished() throws Exception {
        var events = outbox.findAll(Sort.by("id"));
        return objectMapper.readValue(events.getLast().getPayload(), InventoryResult.class);
    }

    @Test
    void reservesStockAndPublishesReservedWithSagaContext() throws Exception {
        reserve("o1", "PHONE", 2);

        assertThat(service.currentStock()).containsEntry("PHONE", 3);
        assertThat(reservationStatus("o1")).isEqualTo(Reservation.Status.RESERVED);
        var event = lastPublished();
        assertThat(event.status()).isEqualTo(InventoryResult.Status.RESERVED);
        assertThat(event.customerId()).isEqualTo("c1");
    }

    @Test
    void rejectsWhenNotEnoughStockOrUnknownProduct() throws Exception {
        reserve("o1", "PHONE", 6);
        assertThat(lastPublished().status()).isEqualTo(InventoryResult.Status.REJECTED);

        reserve("o2", "NOPE", 1);
        assertThat(lastPublished().reason()).isEqualTo("Producto desconocido: NOPE");

        assertThat(service.currentStock()).containsEntry("PHONE", 5);
    }

    @Test
    void duplicateOrderReservesOnlyOnce() {
        reserve("o1", "PHONE", 2);
        reserve("o1", "PHONE", 2);

        assertThat(service.currentStock()).containsEntry("PHONE", 3);
        assertThat(outbox.findAll()).hasSize(1);
    }

    @Test
    void releaseReturnsStockOnlyOnce() {
        reserve("o1", "PHONE", 2);

        service.releaseReservation("o1");
        service.releaseReservation("o1");

        assertThat(service.currentStock()).containsEntry("PHONE", 5);
        assertThat(reservationStatus("o1")).isEqualTo(Reservation.Status.RELEASED);
    }

    @Test
    void confirmKeepsStockReserved() {
        reserve("o1", "PHONE", 2);

        service.confirmReservation("o1");

        assertThat(service.currentStock()).containsEntry("PHONE", 3);
        assertThat(reservationStatus("o1")).isEqualTo(Reservation.Status.CONFIRMED);
    }
}
