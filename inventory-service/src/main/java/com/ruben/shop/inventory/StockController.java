package com.ruben.shop.inventory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StockController {

    private final InventoryService service;

    public StockController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/stock")
    public Map<String, Integer> stock() {
        return service.currentStock();
    }
}
