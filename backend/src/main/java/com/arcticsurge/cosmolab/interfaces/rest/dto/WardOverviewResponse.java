package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.application.ward.WardOverviewService.WardPatientSummary;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WardOverviewResponse(
        String wardId,
        int patientCount,
        List<PatientSummary> patients
) {
    public record PatientSummary(
            UUID patientId,
            UUID ehrId,
            String firstName,
            String lastName,
            PatientStatus status,
            LatestVitals latestVitals,
            long activeProblemCount,
            List<String> flags
    ) {}

    public record LatestVitals(
            Instant recordedAt,
            Integer systolicBp,
            Integer diastolicBp,
            Integer heartRate,
            BigDecimal temperature,
            BigDecimal oxygenSaturation
    ) {}

    public static WardOverviewResponse from(String wardId, List<WardPatientSummary> summaries) {
        List<PatientSummary> patients = summaries.stream().map(s -> new PatientSummary(
                s.patient().getId(),
                s.ehr().getEhrId(),
                s.patient().getFirstName(),
                s.patient().getLastName(),
                s.patient().getStatus(),
                s.latestVitals() == null ? null : new LatestVitals(
                        s.latestVitals().getRecordedAt(),
                        s.latestVitals().getSystolicBp(),
                        s.latestVitals().getDiastolicBp(),
                        s.latestVitals().getHeartRate(),
                        s.latestVitals().getTemperature(),
                        s.latestVitals().getOxygenSaturation()
                ),
                s.activeProblemCount(),
                s.flags()
        )).toList();
        return new WardOverviewResponse(wardId, patients.size(), patients);
    }
}
