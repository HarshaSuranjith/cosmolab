package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Request body for creating or updating a patient")
public record PatientRequest(
        @Schema(description = "Patient's given name", example = "Anna")
        @NotBlank @Size(max = 100) String firstName,

        @Schema(description = "Patient's family name", example = "Lindström")
        @NotBlank @Size(max = 100) String lastName,

        @Schema(description = "Swedish personal identity number — YYYYMMDD-XXXX", example = "19850315-1234")
        @NotBlank @Pattern(regexp = "\\d{8}-\\d{4}", message = "must be YYYYMMDD-XXXX") String personalNumber,

        @Schema(description = "Date of birth", example = "1985-03-15")
        @NotNull LocalDate dateOfBirth,

        @Schema(description = "Biological sex")
        @NotNull Gender gender,

        @Schema(description = "Ward or unit where the patient is admitted", example = "ICU")
        @NotBlank @Size(max = 100) String ward,

        @Schema(description = "Current admission status")
        @NotNull PatientStatus status
) {}
