package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.observation.VitalSigns;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VitalSignsResponse(
        UUID id,
        UUID compositionId,
        Instant recordedAt,
        UUID recordedBy,
        Integer systolicBp,
        Integer diastolicBp,
        Integer heartRate,
        Integer respiratoryRate,
        BigDecimal temperature,
        BigDecimal oxygenSaturation,
        BigDecimal weight
) {
    public static VitalSignsResponse from(VitalSigns v) {
        return new VitalSignsResponse(v.getId(), v.getCompositionId(), v.getRecordedAt(),
                v.getRecordedBy(), v.getSystolicBp(), v.getDiastolicBp(), v.getHeartRate(),
                v.getRespiratoryRate(), v.getTemperature(), v.getOxygenSaturation(), v.getWeight());
    }
}
