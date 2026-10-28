package com.ruben.shop.notification;

import com.ruben.shop.common.Topics;
import com.ruben.shop.common.events.OrderStatusChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication(scanBasePackages = "com.ruben.shop")
public class NotificationServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    // shortcut: solo escribe en el log; conectar a SMTP/SendGrid para enviar emails reales.
    @KafkaListener(topics = Topics.ORDER_STATUS)
    public void onOrderStatus(OrderStatusChanged event) {
        log.info("📧 Email a {}: tu pedido {} está {}{}", event.customerId(), event.orderId(), event.status(),
                event.reason() == null ? "" : " — motivo: " + event.reason());
    }
}
