package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request body for creating or updating a clinical composition")
public record CompositionRequest(
        @Schema(description = "Clinical document type — determines which entry archetypes can be nested inside")
        @NotNull CompositionType type,

        @Schema(description = "UUID of the clinician authoring this composition")
        @NotNull UUID authorId,

        @Schema(description = "Clinical event time (ISO 8601 Instant) — when the act occurred, not when it was committed",
                example = "2024-06-01T08:30:00Z")
        Instant startTime,

        @Schema(description = "Name of the healthcare facility", example = "Karolinska Universitetssjukhuset")
        String facilityName
) {}
