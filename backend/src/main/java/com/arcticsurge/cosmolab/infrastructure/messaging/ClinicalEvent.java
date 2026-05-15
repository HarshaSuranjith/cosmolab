package com.arcticsurge.cosmolab.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

public record ClinicalEvent(
        String eventId,
        String eventType,
        UUID aggregateId,
        String payload,
        Instant occurredAt
) {
    public static ClinicalEvent of(String eventType, UUID aggregateId, String payload) {
        return new ClinicalEvent(
                UUID.randomUUID().toString(),
                eventType,
                aggregateId,
                payload,
                Instant.now()
        );
    }
}
