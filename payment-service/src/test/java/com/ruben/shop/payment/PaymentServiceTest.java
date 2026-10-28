package com.ruben.shop.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruben.shop.common.events.PaymentResult;
import com.ruben.shop.outbox.OutboxEventRepository;
import com.ruben.shop.payment.application.PaymentService;
import com.ruben.shop.payment.application.ProcessPaymentCommand;
import com.ruben.shop.payment.domain.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Caso de uso con sus adaptadores reales (JPA, outbox y la pasarela simulada) sobre H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

    @Autowired
    PaymentService service;
    @Autowired
    PaymentRepository payments;
    @Autowired
    OutboxEventRepository outbox;
    @Autowired
    ObjectMapper objectMapper;

    private void pay(String customer, String amount) {
        service.processPayment(new ProcessPaymentCommand("o1", customer, new BigDecimal(amount)));
    }

    private PaymentResult published() throws Exception {
        return objectMapper.readValue(outbox.findAll().getFirst().getPayload(), PaymentResult.class);
    }

    @Test
    void completedPaymentIsStoredAndPublished() throws Exception {
        pay("c1", "1000");

        assertThat(payments.exists("o1")).isTrue();
        assertThat(published()).isEqualTo(new PaymentResult("o1", PaymentResult.Status.COMPLETED, null));
    }

    @Test
    void failedPaymentIsPublishedWithReason() throws Exception {
        pay("c1", "1000.01");

        assertThat(published()).isEqualTo(
                new PaymentResult("o1", PaymentResult.Status.FAILED, "Importe supera el límite de 1000"));
    }

    @Test
    void duplicatesAreChargedOnlyOnce() {
        pay("c1", "10");
        pay("c1", "10");

        assertThat(outbox.count()).isEqualTo(1);
    }

    @Test
    void gatewayFailureThrowsSoKafkaRetriesAndNothingIsStored() {
        assertThatThrownBy(() -> pay("boom", "10")).isInstanceOf(IllegalStateException.class);

        assertThat(payments.exists("o1")).isFalse();
        assertThat(outbox.count()).isZero();
    }

    @Test
    void failPaymentPublishesFailureSoTheSagaCompensates() throws Exception {
        service.failPayment(new ProcessPaymentCommand("o1", "boom", BigDecimal.TEN), "Pasarela caída");

        assertThat(payments.exists("o1")).isTrue();
        assertThat(published()).isEqualTo(new PaymentResult("o1", PaymentResult.Status.FAILED, "Pasarela caída"));
    }

    @Test
    void failPaymentIgnoresOrdersAlreadyPaid() {
        pay("c1", "10");
        service.failPayment(new ProcessPaymentCommand("o1", "c1", BigDecimal.TEN), "Pasarela caída");

        assertThat(outbox.count()).isEqualTo(1);
    }
}
