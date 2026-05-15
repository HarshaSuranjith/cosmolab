package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.evaluation.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Request body for adding or updating a problem list entry")
public record ProblemRequest(
        @Schema(description = "Composition this problem is recorded under")
        @NotNull UUID compositionId,

        @Schema(description = "ICD-10 diagnostic code", example = "J18.9")
        @NotBlank @Size(max = 20) String icd10Code,

        @Schema(description = "Human-readable diagnosis name", example = "Community-acquired pneumonia")
        @NotBlank @Size(max = 200) String displayName,

        @Schema(description = "Clinical severity — MILD | MODERATE | SEVERE")
        @NotNull Severity severity,

        @Schema(description = "UUID of the clinician recording this problem")
        @NotNull UUID recordedBy,

        @Schema(description = "Date symptoms or diagnosis began", example = "2024-05-28")
        LocalDate onsetDate
) {}
