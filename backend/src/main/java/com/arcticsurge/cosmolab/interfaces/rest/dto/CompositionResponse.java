package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.composition.Composition;
import com.arcticsurge.cosmolab.domain.composition.CompositionStatus;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Clinical composition — openEHR COMPOSITION archetype")
public record CompositionResponse(
        @Schema(description = "Composition identifier") UUID id,
        @Schema(description = "EHR this composition belongs to") UUID ehrId,
        @Schema(description = "Clinical document type") CompositionType type,
        @Schema(description = "Authoring clinician UUID") UUID authorId,
        @Schema(description = "Clinical event time (when the act occurred)") Instant startTime,
        @Schema(description = "System commit time (when the record was persisted)") Instant commitTime,
        @Schema(description = "Healthcare facility name") String facilityName,
        @Schema(description = "Composition lifecycle status") CompositionStatus status
) {
    public static CompositionResponse from(Composition c) {
        return new CompositionResponse(c.getId(), c.getEhrId(), c.getType(),
                c.getAuthorId(), c.getStartTime(), c.getCommitTime(),
                c.getFacilityName(), c.getStatus());
    }
}
