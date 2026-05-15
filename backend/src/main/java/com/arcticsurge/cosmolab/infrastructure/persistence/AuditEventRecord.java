package com.arcticsurge.cosmolab.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEventRecord {

    @Id
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "event_id", columnDefinition = "NVARCHAR(36)", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String eventType;

    @Column(name = "aggregate_id", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID aggregateId;

    @Column(name = "payload", columnDefinition = "NVARCHAR(MAX)")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (processedAt == null) processedAt = Instant.now();
    }
}
