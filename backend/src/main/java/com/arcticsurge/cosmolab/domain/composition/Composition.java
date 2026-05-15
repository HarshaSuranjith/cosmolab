package com.arcticsurge.cosmolab.domain.composition;

import com.arcticsurge.cosmolab.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compositions")
@Getter
@Setter
@NoArgsConstructor
public class Composition extends BaseEntity {

    @Column(name = "ehr_id", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID ehrId;

    @Enumerated(EnumType.STRING)
    @Column(name = "composition_type", columnDefinition = "NVARCHAR(50)", nullable = false)
    private CompositionType type;

    @Column(name = "author_id", columnDefinition = "UNIQUEIDENTIFIER", nullable = false)
    private UUID authorId;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "commit_time", nullable = false)
    private Instant commitTime;

    @Column(name = "facility_name", columnDefinition = "NVARCHAR(200)")
    private String facilityName;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "NVARCHAR(20)", nullable = false)
    private CompositionStatus status;

    @PrePersist
    void onCreate() {
        commitTime = Instant.now();
        if (startTime == null) startTime = commitTime;
        if (status == null) status = CompositionStatus.COMPLETE;
    }
}
