package com.ruben.shop.order;

import com.ruben.shop.common.Topics;
import com.ruben.shop.order.application.OrderService;
import com.ruben.shop.order.application.PlaceOrderCommand;
import com.ruben.shop.order.domain.Money;
import com.ruben.shop.order.domain.Order;
import com.ruben.shop.order.domain.OrderStatus;
import com.ruben.shop.outbox.OutboxEvent;
import com.ruben.shop.outbox.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/** Casos de uso con sus adaptadores reales (JPA + outbox) sobre H2. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceTest {

    @Autowired
    OrderService service;
    @Autowired
    OutboxEventRepository outbox;

    private Order place(int quantity) {
        return service.placeOrder(new PlaceOrderCommand("c1", "LAPTOP", quantity, Money.of("900")));
    }

    private OrderStatus statusOf(Order order) {
        return service.find(order.id()).orElseThrow().status();
    }

    @Test
    void placeOrderStoresOrderAndOutboxEventInSameTransaction() {
        var order = place(1);

        assertThat(statusOf(order)).isEqualTo(OrderStatus.PENDING);
        assertThat(outbox.findAll()).singleElement().satisfies(e -> {
            assertThat(e.getTopic()).isEqualTo(Topics.ORDER_CREATED);
            assertThat(e.getEventKey()).isEqualTo(order.id().value());
        });
    }

    @Test
    void confirmPersistsAndPublishesStatusChange() {
        var order = place(1);

        service.confirm(order.id());

        assertThat(statusOf(order)).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(outbox.findAll(Sort.by("id"))).extracting(OutboxEvent::getTopic)
                .containsExactly(Topics.ORDER_CREATED, Topics.ORDER_STATUS);
    }

    @Test
    void duplicateEventsDoNotChangeFinalStateNorRepublish() {
        var order = place(1);

        service.confirm(order.id());
        service.confirm(order.id());
        service.cancel(order.id(), "tarde");

        var stored = service.find(order.id()).orElseThrow();
        assertThat(stored.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(stored.reason()).isNull();
        assertThat(outbox.findAll()).filteredOn(e -> e.getTopic().equals(Topics.ORDER_STATUS)).hasSize(1);
    }

    @Test
    void findByCustomerOnlyReturnsTheirOrders() {
        place(1);
        service.placeOrder(new PlaceOrderCommand("otro", "PHONE", 1, Money.of("10")));

        assertThat(service.findByCustomer("c1")).extracting(Order::customerId).containsOnly("c1");
    }
}
