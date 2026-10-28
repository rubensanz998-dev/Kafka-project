package com.ruben.shop.inventory.domain;

/** Agregado: un producto y su stock disponible. El stock nunca puede quedar negativo. */
public class Product {

    private final String id;
    private int stock;

    public Product(String id, int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        this.id = id;
        this.stock = stock;
    }

    /** Si hay stock suficiente lo descuenta y devuelve una reserva RESERVED; si no, una REJECTED con el motivo. */
    public Reservation reserve(String orderId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("La cantidad debe ser al menos 1");
        }
        if (stock < quantity) {
            return Reservation.rejected(orderId, id, quantity,
                    "Stock insuficiente de " + id + " (disponible: " + stock + ")");
        }
        stock -= quantity;
        return Reservation.reserved(orderId, id, quantity);
    }

    /** Compensación: devuelve al stock lo que una reserva había apartado. */
    public void restock(int quantity) {
        stock += quantity;
    }

    public String id() {
        return id;
    }

    public int stock() {
        return stock;
    }
}
