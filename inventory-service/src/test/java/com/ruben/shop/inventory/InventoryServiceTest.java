package com.ruben.shop.inventory;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.common.events.OrderCreated;
import com.ruben.shop.common.events.PaymentResult;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InventoryServiceTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final InventoryService service = new InventoryService(kafka);

    private static OrderCreated order(String id, int quantity) {
        return new OrderCreated(id, "c1", "PHONE", quantity, new BigDecimal("300"));
    }

    @Test
    void reservesStockWhenAvailable() {
        service.onOrderCreated(order("o1", 2));

        assertThat(service.currentStock()).containsEntry("PHONE", 3);
        verify(kafka).send(eq(Topics.INVENTORY_RESULT), eq("o1"),
                argThat(e -> ((InventoryResult) e).status() == InventoryResult.Status.RESERVED));
    }

    @Test
    void rejectsWhenNotEnoughStock() {
        service.onOrderCreated(order("o1", 6));

        assertThat(service.currentStock()).containsEntry("PHONE", 5);
        verify(kafka).send(eq(Topics.INVENTORY_RESULT), eq("o1"),
                argThat(e -> ((InventoryResult) e).status() == InventoryResult.Status.REJECTED));
    }

    @Test
    void duplicateOrderReservesOnlyOnce() {
        service.onOrderCreated(order("o1", 2));
        service.onOrderCreated(order("o1", 2));

        assertThat(service.currentStock()).containsEntry("PHONE", 3);
    }

    @Test
    void failedPaymentReleasesStockOnlyOnce() {
        service.onOrderCreated(order("o1", 2));
        var failed = new PaymentResult("o1", PaymentResult.Status.FAILED, "rechazado");

        service.onPaymentResult(failed);
        service.onPaymentResult(failed);

        assertThat(service.currentStock()).containsEntry("PHONE", 5);
    }

    @Test
    void completedPaymentKeepsStockReserved() {
        service.onOrderCreated(order("o1", 2));

        service.onPaymentResult(new PaymentResult("o1", PaymentResult.Status.COMPLETED, null));

        assertThat(service.currentStock()).containsEntry("PHONE", 3);
    }
}
