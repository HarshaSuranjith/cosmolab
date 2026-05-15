package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.application.ward.WardOverviewService.WardPatientSummary;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Ward dashboard response — aggregated patient summaries with latest vitals and active problem counts")
public record WardOverviewResponse(
        @Schema(description = "Ward identifier", example = "ICU") String wardId,
        @Schema(description = "Total number of active patients in the ward") int patientCount,
        @Schema(description = "Per-patient summaries") List<PatientSummary> patients
) {
    @Schema(description = "Per-patient summary row on the ward dashboard")
    public record PatientSummary(
            @Schema(description = "Patient identifier") UUID patientId,
            @Schema(description = "EHR root identifier") UUID ehrId,
            @Schema(description = "Patient's given name") String firstName,
            @Schema(description = "Patient's family name") String lastName,
            @Schema(description = "Current admission status") PatientStatus status,
            @Schema(description = "Most recent vital signs — null if none recorded") LatestVitals latestVitals,
            @Schema(description = "Count of ACTIVE problem list entries") long activeProblemCount,
            @Schema(description = "Abnormality flags, e.g. HIGH_BP, LOW_SPO2, FEVER") List<String> flags
    ) {}

    @Schema(description = "Snapshot of the most recent vital signs for ward display")
    public record LatestVitals(
            @Schema(description = "When the measurement was taken") Instant recordedAt,
            @Schema(description = "Systolic blood pressure in mmHg") Integer systolicBp,
            @Schema(description = "Diastolic blood pressure in mmHg") Integer diastolicBp,
            @Schema(description = "Heart rate in beats per minute") Integer heartRate,
            @Schema(description = "Body temperature in °C") BigDecimal temperature,
            @Schema(description = "Peripheral oxygen saturation (SpO₂) in %") BigDecimal oxygenSaturation
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
