package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.composition.Composition;
import com.arcticsurge.cosmolab.domain.composition.CompositionStatus;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;

import java.time.Instant;
import java.util.UUID;

public record CompositionResponse(
        UUID id,
        UUID ehrId,
        CompositionType type,
        UUID authorId,
        Instant startTime,
        Instant commitTime,
        String facilityName,
        CompositionStatus status
) {
    public static CompositionResponse from(Composition c) {
        return new CompositionResponse(c.getId(), c.getEhrId(), c.getType(),
                c.getAuthorId(), c.getStartTime(), c.getCommitTime(),
                c.getFacilityName(), c.getStatus());
    }
}
