package com.example.saga.entity;

import com.example.saga.event.SagaEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_store")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventStore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String eventType;

    @Lob
    @Column(nullable = false)
    private String eventData;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long aggregateVersion;

    public EventStore(Long orderId, String eventType, String eventData, Long aggregateVersion) {
        this.orderId = orderId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.aggregateVersion = aggregateVersion;
        this.createdAt = LocalDateTime.now();
    }
}
