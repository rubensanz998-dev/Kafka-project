package com.ruben.shop.order;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.OrderCreated;
import com.ruben.shop.order.application.OrderService;
import com.ruben.shop.order.application.PlaceOrderCommand;
import com.ruben.shop.order.domain.Money;
import com.ruben.shop.outbox.OutboxEventRepository;
import com.ruben.shop.outbox.OutboxPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxPublisherTest {

    @MockitoBean
    KafkaTemplate<String, Object> kafka;
    @Autowired
    OrderService orders;
    @Autowired
    OutboxPublisher publisher;
    @Autowired
    OutboxEventRepository outbox;

    private String place(String customer) {
        return orders.placeOrder(new PlaceOrderCommand(customer, "LAPTOP", 1, Money.of("10"))).id().value();
    }

    @Test
    void publishesPendingEventsInOrderAndMarksThemSent() throws Exception {
        when(kafka.send(anyString(), anyString(), any())).thenReturn(CompletableFuture.completedFuture(null));
        var first = place("c1");
        var second = place("c2");

        publisher.publishPending();

        var inOrder = inOrder(kafka);
        inOrder.verify(kafka).send(eq(Topics.ORDER_CREATED), eq(first), any(OrderCreated.class));
        inOrder.verify(kafka).send(eq(Topics.ORDER_CREATED), eq(second), any(OrderCreated.class));
        assertThat(outbox.findAll()).allSatisfy(e -> assertThat(e.getSentAt()).isNotNull());
    }

    @Test
    void kafkaDownLeavesEventsPending() {
        when(kafka.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));
        place("c1");

        assertThatThrownBy(publisher::publishPending).hasRootCauseMessage("broker caído");

        // En producción la transacción hace rollback; aquí basta con comprobar que no se marcó como enviado.
        assertThat(outbox.findAll()).allSatisfy(e -> assertThat(e.getSentAt()).isNull());
    }
}
