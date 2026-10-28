package com.ruben.shop.payment;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.InventoryResult;
import com.ruben.shop.common.events.PaymentResult;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PaymentServiceTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
    private final PaymentService service = new PaymentService(kafka);

    private static InventoryResult reserved(String customer, String amount) {
        return new InventoryResult("o1", customer, new BigDecimal(amount), InventoryResult.Status.RESERVED, null);
    }

    @Test
    void chargesUpToLimit() {
        service.onInventoryResult(reserved("c1", "1000"));

        verify(kafka).send(Topics.PAYMENT_RESULT, "o1", new PaymentResult("o1", PaymentResult.Status.COMPLETED, null));
    }

    @Test
    void failsAboveLimit() {
        service.onInventoryResult(reserved("c1", "1000.01"));

        verify(kafka).send(Topics.PAYMENT_RESULT, "o1",
                new PaymentResult("o1", PaymentResult.Status.FAILED, "Importe supera el límite de 1000"));
    }

    @Test
    void ignoresRejectedAndDuplicates() {
        service.onInventoryResult(new InventoryResult("o2", "c1", BigDecimal.TEN, InventoryResult.Status.REJECTED, "x"));
        service.onInventoryResult(reserved("c1", "10"));
        service.onInventoryResult(reserved("c1", "10"));

        verify(kafka, times(1)).send(eq(Topics.PAYMENT_RESULT), anyString(), any());
    }

    @Test
    void technicalErrorThrowsSoErrorHandlerCanRetryAndSendToDlt() {
        assertThatThrownBy(() -> service.onInventoryResult(reserved(PaymentService.FAILING_CUSTOMER, "10")))
                .isInstanceOf(IllegalStateException.class);
        verify(kafka, never()).send(anyString(), anyString(), any());
    }
}
