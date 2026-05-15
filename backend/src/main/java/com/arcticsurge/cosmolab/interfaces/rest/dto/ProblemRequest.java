package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.evaluation.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ProblemRequest(
        @NotNull UUID compositionId,
        @NotBlank @Size(max = 20) String icd10Code,
        @NotBlank @Size(max = 200) String displayName,
        @NotNull Severity severity,
        @NotNull UUID recordedBy,
        LocalDate onsetDate
) {}
