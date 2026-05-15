package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Pattern(regexp = "\\d{8}-\\d{4}", message = "must be YYYYMMDD-XXXX") String personalNumber,
        @NotNull LocalDate dateOfBirth,
        @NotNull Gender gender,
        @NotBlank @Size(max = 100) String ward,
        @NotNull PatientStatus status
) {}
