package com.ruben.shop.order;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.common.events.OrderCreated;
import com.ruben.shop.common.events.PaymentResult;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class OrderServiceTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final OrderService service = new OrderService(kafka);

    @Test
    void createPublishesOrderCreatedKeyedByOrderId() {
        var order = service.create("c1", "LAPTOP", 1, new BigDecimal("900"));

        assertThat(order.status()).isEqualTo(Order.Status.PENDING);
        verify(kafka).send(eq(Topics.ORDER_CREATED), eq(order.id()), any(OrderCreated.class));
    }

    @Test
    void paymentCompletedConfirmsOrder() {
        var order = service.create("c1", "LAPTOP", 1, new BigDecimal("900"));

        service.onPaymentResult(new PaymentResult(order.id(), PaymentResult.Status.COMPLETED, null));

        assertThat(service.find(order.id()).orElseThrow().status()).isEqualTo(Order.Status.CONFIRMED);
    }

    @Test
    void stockRejectedCancelsOrder() {
        var order = service.create("c1", "LAPTOP", 99, new BigDecimal("900"));

        service.onInventoryResult(new InventoryResult(order.id(), "c1", order.amount(),
                InventoryResult.Status.REJECTED, "sin stock"));

        var updated = service.find(order.id()).orElseThrow();
        assertThat(updated.status()).isEqualTo(Order.Status.CANCELLED);
        assertThat(updated.reason()).isEqualTo("sin stock");
    }

    @Test
    void duplicateEventsDoNotChangeFinalStateNorRepublish() {
        var order = service.create("c1", "LAPTOP", 1, new BigDecimal("900"));
        var completed = new PaymentResult(order.id(), PaymentResult.Status.COMPLETED, null);

        service.onPaymentResult(completed);
        service.onPaymentResult(completed);
        service.onPaymentResult(new PaymentResult(order.id(), PaymentResult.Status.FAILED, "tarde"));

        assertThat(service.find(order.id()).orElseThrow().status()).isEqualTo(Order.Status.CONFIRMED);
        verify(kafka, times(1)).send(eq(Topics.ORDER_STATUS), anyString(), any());
    }
}
