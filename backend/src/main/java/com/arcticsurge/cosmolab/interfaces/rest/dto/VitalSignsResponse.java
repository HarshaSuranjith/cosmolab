package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.observation.VitalSigns;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Vital signs observation — openEHR OBSERVATION archetype")
public record VitalSignsResponse(
        @Schema(description = "Observation identifier") UUID id,
        @Schema(description = "Parent composition identifier") UUID compositionId,
        @Schema(description = "Timestamp of measurement") Instant recordedAt,
        @Schema(description = "Clinician who recorded the measurement") UUID recordedBy,
        @Schema(description = "Systolic blood pressure in mmHg") Integer systolicBp,
        @Schema(description = "Diastolic blood pressure in mmHg") Integer diastolicBp,
        @Schema(description = "Heart rate in beats per minute") Integer heartRate,
        @Schema(description = "Respiratory rate in breaths per minute") Integer respiratoryRate,
        @Schema(description = "Body temperature in °C") BigDecimal temperature,
        @Schema(description = "Peripheral oxygen saturation (SpO₂) in %") BigDecimal oxygenSaturation,
        @Schema(description = "Body weight in kg") BigDecimal weight
) {
    public static VitalSignsResponse from(VitalSigns v) {
        return new VitalSignsResponse(v.getId(), v.getCompositionId(), v.getRecordedAt(),
                v.getRecordedBy(), v.getSystolicBp(), v.getDiastolicBp(), v.getHeartRate(),
                v.getRespiratoryRate(), v.getTemperature(), v.getOxygenSaturation(), v.getWeight());
    }
}
