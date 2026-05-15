package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "vw_ward_patient_summary")
@Getter
@NoArgsConstructor
public class WardOverviewRecord {

    @Id
    @Column(name = "patient_id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID patientId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "personal_number")
    private String personalNumber;

    @Column(name = "ward")
    private String ward;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_status")
    private PatientStatus patientStatus;

    @Column(name = "ehr_id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID ehrId;

    // Null when the patient has no vital signs recorded yet
    @Column(name = "vitals_recorded_at")
    private Instant vitalsRecordedAt;

    @Column(name = "systolic_bp")
    private Integer systolicBp;

    @Column(name = "diastolic_bp")
    private Integer diastolicBp;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "temperature", precision = 4, scale = 1)
    private BigDecimal temperature;

    @Column(name = "oxygen_saturation", precision = 5, scale = 2)
    private BigDecimal oxygenSaturation;

    @Column(name = "weight", precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "active_problem_count")
    private Integer activeProblemCount;
}