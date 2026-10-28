package com.ruben.shop.order.infrastructure.web;

import com.ruben.shop.order.application.OrderService;
import com.ruben.shop.order.application.PlaceOrderCommand;
import com.ruben.shop.order.domain.Money;
import com.ruben.shop.order.domain.Order;
import com.ruben.shop.order.domain.OrderId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Adaptador de entrada HTTP. Traduce JSON ↔ comandos/DTOs; nunca expone el agregado directamente. */
@RestController
@RequestMapping("/orders")
class OrderController {

    /** Sin customerId: el cliente es siempre el usuario del token, nadie puede comprar en nombre de otro. */
    record CreateOrderRequest(@NotBlank String productId,
                              @Min(1) int quantity,
                              @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }

    record OrderResponse(String id, String customerId, String productId, int quantity, BigDecimal amount,
                         String status, String reason, Instant createdAt, Instant updatedAt) {
        static OrderResponse from(Order o) {
            return new OrderResponse(o.id().value(), o.customerId(), o.productId(), o.quantity(),
                    o.amount().value(), o.status().name(), o.reason(), o.createdAt(), o.updatedAt());
        }
    }

    private final OrderService orders;

    OrderController(OrderService orders) {
        this.orders = orders;
    }

    private static String customer(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }

    /** 202 Accepted: el pedido se procesa de forma asíncrona; consulta GET /orders/{id} para ver el estado final. */
    @PostMapping
    ResponseEntity<OrderResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateOrderRequest req) {
        var order = orders.placeOrder(new PlaceOrderCommand(customer(jwt), req.productId(), req.quantity(),
                new Money(req.amount())));
        return ResponseEntity.accepted().body(OrderResponse.from(order));
    }

    /** 404 también si el pedido es de otro usuario: no revela que existe. */
    @GetMapping("/{id}")
    ResponseEntity<OrderResponse> get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return ResponseEntity.of(orders.find(new OrderId(id))
                .filter(o -> o.belongsTo(customer(jwt)))
                .map(OrderResponse::from));
    }

    @GetMapping
    List<OrderResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return orders.findByCustomer(customer(jwt)).stream().map(OrderResponse::from).toList();
    }
}
