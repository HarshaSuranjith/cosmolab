package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record CompositionRequest(
        @NotNull CompositionType type,
        @NotNull UUID authorId,
        Instant startTime,
        String facilityName
) {}
