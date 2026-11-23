package com.example.Backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_requests")
public class NotificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false, length = 4000)
    private String payload;

    @Column(nullable = false)
    private Instant receivedAt;

    protected NotificationRequest() {
    }

    public NotificationRequest(String type, String payload, Instant receivedAt) {
        this.type = type;
        this.payload = payload;
        this.receivedAt = receivedAt;
    }

    public Long getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}
