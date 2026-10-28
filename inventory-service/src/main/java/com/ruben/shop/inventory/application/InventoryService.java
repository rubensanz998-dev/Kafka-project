package com.ruben.shop.inventory.application;

import com.ruben.shop.inventory.domain.Product;
import com.ruben.shop.inventory.domain.ProductRepository;
import com.ruben.shop.inventory.domain.Reservation;
import com.ruben.shop.inventory.domain.ReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.TreeMap;

/** Casos de uso del inventario: reservar stock, confirmar la reserva y liberarla (compensación). */
@Service
@Transactional
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository products;
    private final ReservationRepository reservations;
    private final InventoryEventPublisher events;

    public InventoryService(ProductRepository products, ReservationRepository reservations,
                            InventoryEventPublisher events) {
        this.products = products;
        this.reservations = reservations;
        this.events = events;
    }

    public void reserveStock(ReserveStockCommand cmd) {
        // Kafka entrega at-least-once: sin esta comprobación un reintento reservaría stock dos veces.
        // Si dos copias llegan a la vez, la PK de reservation hace fallar a una y su reintento cae aquí.
        if (reservations.exists(cmd.orderId())) {
            log.warn("Pedido {} duplicado, ignorado", cmd.orderId());
            return;
        }
        var reservation = products.findById(cmd.productId())
                .map(product -> reserve(product, cmd))
                .orElseGet(() -> Reservation.rejected(cmd.orderId(), cmd.productId(), cmd.quantity(),
                        "Producto desconocido: " + cmd.productId()));
        reservations.save(reservation);
        log.info("Pedido {}: {}{}", cmd.orderId(), reservation.status(),
                reservation.reason() == null ? "" : " (" + reservation.reason() + ")");
        events.reservationDecided(reservation, cmd);
    }

    public void confirmReservation(String orderId) {
        reservations.findByOrderId(orderId).filter(Reservation::confirm).ifPresent(reservations::save);
    }

    public void releaseReservation(String orderId) {
        reservations.findByOrderId(orderId).filter(Reservation::release).ifPresent(reservation -> {
            var product = products.findById(reservation.productId()).orElseThrow();
            product.restock(reservation.quantity());
            products.save(product);
            reservations.save(reservation);
            log.info("Compensación: pedido {} liberó {} uds de {}", orderId, reservation.quantity(), product.id());
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Integer> currentStock() {
        var stock = new TreeMap<String, Integer>();
        products.findAll().forEach(p -> stock.put(p.id(), p.stock()));
        return stock;
    }

    private Reservation reserve(Product product, ReserveStockCommand cmd) {
        var reservation = product.reserve(cmd.orderId(), cmd.quantity());
        products.save(product);
        return reservation;
    }
}
