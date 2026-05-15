package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemListEntry;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import com.arcticsurge.cosmolab.domain.evaluation.Severity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProblemResponse(
        UUID id,
        UUID ehrId,
        UUID compositionId,
        String icd10Code,
        String displayName,
        Severity severity,
        ProblemStatus status,
        LocalDate onsetDate,
        LocalDate resolvedDate,
        Instant recordedAt
) {
    public static ProblemResponse from(ProblemListEntry e) {
        return new ProblemResponse(e.getId(), e.getEhrId(), e.getCompositionId(),
                e.getIcd10Code(), e.getDisplayName(), e.getSeverity(), e.getStatus(),
                e.getOnsetDate(), e.getResolvedDate(), e.getRecordedAt());
    }
}
