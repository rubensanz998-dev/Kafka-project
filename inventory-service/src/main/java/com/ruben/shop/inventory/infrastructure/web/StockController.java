package com.ruben.shop.inventory.infrastructure.web;

import com.ruben.shop.inventory.application.InventoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoint público de solo lectura: el catálogo de stock no necesita login. */
@RestController
class StockController {

    private final InventoryService inventory;

    StockController(InventoryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping("/stock")
    Map<String, Integer> stock() {
        return inventory.currentStock();
    }
}
