package com.ruben.shop.outbox;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String topic;
    private String eventKey;
    private String payloadType;
    private String payload;
    private Instant createdAt;
    private Instant sentAt;

    protected OutboxEvent() {
    }

    OutboxEvent(String topic, String eventKey, String payloadType, String payload) {
        this.topic = topic;
        this.eventKey = eventKey;
        this.payloadType = payloadType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    void markSent() {
        this.sentAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getEventKey() {
        return eventKey;
    }

    public String getPayloadType() {
        return payloadType;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
