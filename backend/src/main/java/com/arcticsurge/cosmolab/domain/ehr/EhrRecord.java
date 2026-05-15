package com.arcticsurge.cosmolab.domain.ehr;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ehr_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class EhrRecord {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "ehr_id", columnDefinition = "UNIQUEIDENTIFIER")
    private UUID ehrId;

    @Column(name = "subject_id", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID subjectId;

    @Column(name = "system_id", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String systemId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "NVARCHAR(20)", nullable = false)
    private EhrStatus status;

    @PrePersist
    void onCreate() {
        if (systemId == null) systemId = "cosmolab-v1";
        if (status == null) status = EhrStatus.ACTIVE;
    }
}
