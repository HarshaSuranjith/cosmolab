package com.arcticsurge.cosmolab.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Request body for recording a vital signs observation")
public record VitalSignsRequest(
        @Schema(description = "Timestamp of measurement — defaults to request time if omitted", example = "2024-06-01T08:30:00Z")
        Instant recordedAt,

        @Schema(description = "UUID of the clinician recording the vitals")
        @NotNull UUID recordedBy,

        @Schema(description = "Systolic blood pressure in mmHg", example = "120")
        Integer systolicBp,

        @Schema(description = "Diastolic blood pressure in mmHg", example = "80")
        Integer diastolicBp,

        @Schema(description = "Heart rate in beats per minute", example = "72")
        Integer heartRate,

        @Schema(description = "Respiratory rate in breaths per minute", example = "16")
        Integer respiratoryRate,

        @Schema(description = "Body temperature in °C", example = "37.1")
        BigDecimal temperature,

        @Schema(description = "Peripheral oxygen saturation (SpO₂) as a percentage", example = "98.5")
        BigDecimal oxygenSaturation,

        @Schema(description = "Body weight in kg", example = "72.5")
        BigDecimal weight
) {}
