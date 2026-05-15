package com.arcticsurge.cosmolab.interfaces.rest.dto;

import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String firstName,
        String lastName,
        String personalNumber,
        LocalDate dateOfBirth,
        Gender gender,
        String ward,
        PatientStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PatientResponse from(Patient p) {
        return new PatientResponse(p.getId(), p.getFirstName(), p.getLastName(),
                p.getPersonalNumber(), p.getDateOfBirth(), p.getGender(),
                p.getWard(), p.getStatus(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
