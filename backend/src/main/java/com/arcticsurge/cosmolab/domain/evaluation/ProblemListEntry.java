package com.arcticsurge.cosmolab.domain.evaluation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "problem_list_entries")
@Getter
@Setter
@NoArgsConstructor
public class ProblemListEntry {

    @Id
    @Column(columnDefinition = "UNIQUEIDENTIFIER")
    private UUID id;

    @Column(name = "composition_id", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID compositionId;

    @Column(name = "ehr_id", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID ehrId;

    @Column(name = "icd10_code", columnDefinition = "NVARCHAR(20)", nullable = false)
    private String icd10Code;

    @Column(name = "display_name", columnDefinition = "NVARCHAR(200)", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "NVARCHAR(20)", nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "NVARCHAR(20)", nullable = false)
    private ProblemStatus status;

    @Column(name = "onset_date")
    private LocalDate onsetDate;

    @Column(name = "resolved_date")
    private LocalDate resolvedDate;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "recorded_by", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID recordedBy;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (recordedAt == null) recordedAt = Instant.now();
        if (status == null) status = ProblemStatus.ACTIVE;
    }
}
