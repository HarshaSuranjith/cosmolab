package com.arcticsurge.cosmolab.domain.ehr;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ehr_records")
@Getter
@Setter
@NoArgsConstructor
public class EhrRecord {

    @Id
    @Column(name = "ehr_id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID ehrId;

    @Column(name = "subject_id", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID subjectId;

    @Column(name = "system_id", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String systemId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "NVARCHAR(20)", nullable = false)
    private EhrStatus status;

    @PrePersist
    void onCreate() {
        if (ehrId == null) ehrId = UUID.randomUUID();
        createdAt = LocalDateTime.now();
        if (systemId == null) systemId = "cosmolab-v1";
        if (status == null) status = EhrStatus.ACTIVE;
    }
}
