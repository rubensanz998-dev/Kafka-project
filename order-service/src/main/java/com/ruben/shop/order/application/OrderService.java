package com.ruben.shop.order.application;

import com.ruben.shop.order.domain.Order;
import com.ruben.shop.order.domain.OrderId;
import com.ruben.shop.order.domain.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Casos de uso del pedido. Orquesta (cargar → llamar al dominio → guardar → publicar) pero no decide nada:
 * las reglas están en el agregado {@link Order}. No sabe nada de HTTP, Kafka ni JPA.
 */
@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final OrderEventPublisher events;

    public OrderService(OrderRepository orders, OrderEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    public Order placeOrder(PlaceOrderCommand cmd) {
        var order = Order.place(cmd.customerId(), cmd.productId(), cmd.quantity(), cmd.amount());
        save(order);
        log.info("Pedido {} creado (PENDING)", order.id().value());
        return order;
    }

    public void confirm(OrderId id) {
        apply(id, Order::confirm);
    }

    public void cancel(OrderId id, String reason) {
        apply(id, order -> order.cancel(reason));
    }

    @Transactional(readOnly = true)
    public Optional<Order> find(OrderId id) {
        return orders.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomer(String customerId) {
        return orders.findByCustomer(customerId);
    }

    private void apply(OrderId id, Predicate<Order> transition) {
        orders.findById(id).ifPresentOrElse(order -> {
            if (transition.test(order)) {
                save(order);
                log.info("Pedido {} -> {}", id.value(), order.status());
            } else {
                log.warn("Evento ignorado: pedido {} ya estaba {}", id.value(), order.status());
            }
        }, () -> log.warn("Evento ignorado: pedido {} desconocido", id.value()));
    }

    /** Agregado y eventos en la misma transacción: el adaptador de eventos escribe en la outbox. */
    private void save(Order order) {
        orders.save(order);
        events.publish(order.pullEvents());
    }
}
