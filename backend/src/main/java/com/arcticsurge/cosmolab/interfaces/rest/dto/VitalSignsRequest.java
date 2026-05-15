package com.arcticsurge.cosmolab.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VitalSignsRequest(
        Instant recordedAt,
        @NotNull UUID recordedBy,
        Integer systolicBp,
        Integer diastolicBp,
        Integer heartRate,
        Integer respiratoryRate,
        BigDecimal temperature,
        BigDecimal oxygenSaturation,
        BigDecimal weight
) {}
