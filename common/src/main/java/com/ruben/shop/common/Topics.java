package com.ruben.shop.common;

import java.util.List;

/** Nombres de los topics de la saga. Todos usan el orderId como clave. */
public final class Topics {
    public static final String ORDER_CREATED = "order.created";
    public static final String INVENTORY_RESULT = "inventory.result";
    public static final String PAYMENT_RESULT = "payment.result";
    public static final String ORDER_STATUS = "order.status";

    public static final List<String> ALL = List.of(ORDER_CREATED, INVENTORY_RESULT, PAYMENT_RESULT, ORDER_STATUS);

    /** Sufijo que usa DeadLetterPublishingRecoverer por defecto. */
    public static final String DLT_SUFFIX = "-dlt";

    private Topics() {
    }
}
