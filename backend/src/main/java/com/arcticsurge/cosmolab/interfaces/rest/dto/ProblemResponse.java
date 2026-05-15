package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemListEntry;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import com.arcticsurge.cosmolab.domain.evaluation.Severity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Problem list entry — openEHR EVALUATION archetype")
public record ProblemResponse(
        @Schema(description = "Entry identifier") UUID id,
        @Schema(description = "EHR this entry belongs to") UUID ehrId,
        @Schema(description = "Parent composition identifier") UUID compositionId,
        @Schema(description = "ICD-10 diagnostic code", example = "J18.9") String icd10Code,
        @Schema(description = "Human-readable diagnosis name", example = "Community-acquired pneumonia") String displayName,
        @Schema(description = "Clinical severity") Severity severity,
        @Schema(description = "Problem lifecycle status") ProblemStatus status,
        @Schema(description = "Date symptoms or diagnosis began") LocalDate onsetDate,
        @Schema(description = "Date the problem was resolved — null if still active") LocalDate resolvedDate,
        @Schema(description = "Timestamp when this entry was recorded") Instant recordedAt
) {
    public static ProblemResponse from(ProblemListEntry e) {
        return new ProblemResponse(e.getId(), e.getEhrId(), e.getCompositionId(),
                e.getIcd10Code(), e.getDisplayName(), e.getSeverity(), e.getStatus(),
                e.getOnsetDate(), e.getResolvedDate(), e.getRecordedAt());
    }
}
