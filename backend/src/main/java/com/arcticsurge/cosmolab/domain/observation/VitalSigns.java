package com.arcticsurge.cosmolab.domain.observation;

import com.arcticsurge.cosmolab.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vital_signs")
@Getter
@Setter
@NoArgsConstructor
public class VitalSigns extends BaseEntity {

    @Column(name = "composition_id", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID compositionId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID recordedBy;

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

    @PrePersist
    void onCreate() {
        if (recordedAt == null) recordedAt = Instant.now();
    }
}
