package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Patient demographic record")
public record PatientResponse(
        @Schema(description = "Unique patient identifier") UUID id,
        @Schema(description = "Patient's given name", example = "Anna") String firstName,
        @Schema(description = "Patient's family name", example = "Lindström") String lastName,
        @Schema(description = "Swedish personal identity number", example = "19850315-1234") String personalNumber,
        @Schema(description = "Date of birth", example = "1985-03-15") LocalDate dateOfBirth,
        @Schema(description = "Biological sex") Gender gender,
        @Schema(description = "Current ward", example = "ICU") String ward,
        @Schema(description = "Admission status") PatientStatus status,
        @Schema(description = "Record creation timestamp (UTC)") Instant createdAt,
        @Schema(description = "Record last updated timestamp (UTC)") Instant updatedAt
) {}
