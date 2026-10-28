package com.ruben.shop.payment.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.common.events.PaymentResult;
import com.ruben.shop.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** El listener del DLT con su caso de uso real: convierte un cobro abandonado en compensación de la saga. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeadLetterListenerTest {

    @Autowired
    DeadLetterListener listener;
    @Autowired
    OutboxEventRepository outbox;
    @Autowired
    ObjectMapper objectMapper;

    private void deadLetter(InventoryResult.Status status) throws Exception {
        listener.onDeadLetter(objectMapper.writeValueAsBytes(
                new InventoryResult("o1", "boom", BigDecimal.TEN, status, null)));
    }

    @Test
    void reservedOrderInDltIsPublishedAsFailedPayment() throws Exception {
        deadLetter(InventoryResult.Status.RESERVED);

        var published = objectMapper.readValue(outbox.findAll().getFirst().getPayload(), PaymentResult.class);
        assertThat(published).isEqualTo(
                new PaymentResult("o1", PaymentResult.Status.FAILED, DeadLetterListener.REASON));
    }

    @Test
    void rejectedOrdersAndPoisonPillsAreIgnored() throws Exception {
        deadLetter(InventoryResult.Status.REJECTED);
        listener.onDeadLetter("esto no es json".getBytes(StandardCharsets.UTF_8));
        listener.onDeadLetter(null);
        listener.onDeadLetter("null".getBytes(StandardCharsets.UTF_8));

        assertThat(outbox.count()).isZero();
    }
}
