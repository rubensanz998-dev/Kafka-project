package com.ruben.shop.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Collection;

@RestController
@RequestMapping("/orders")
public class OrderController {

    public record CreateOrderRequest(@NotBlank String customerId,
                                     @NotBlank String productId,
                                     @Min(1) int quantity,
                                     @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /** 202 Accepted: el pedido se procesa de forma asíncrona; consulta GET /orders/{id} para ver el estado final. */
    @PostMapping
    public ResponseEntity<Order> create(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.accepted()
                .body(service.create(req.customerId(), req.productId(), req.quantity(), req.amount()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> get(@PathVariable String id) {
        return ResponseEntity.of(service.find(id));
    }

    @GetMapping
    public Collection<Order> all() {
        return service.findAll();
    }
}
