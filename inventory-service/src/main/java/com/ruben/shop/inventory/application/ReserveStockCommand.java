package com.ruben.shop.inventory.application;

import java.math.BigDecimal;

/** customerId y amount no son del inventario: viajan para que el siguiente paso de la saga los tenga. */
public record ReserveStockCommand(String orderId, String customerId, String productId, int quantity,
                                  BigDecimal amount) {
}
