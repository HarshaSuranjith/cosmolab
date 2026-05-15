package com.arcticsurge.cosmolab.interfaces.rest.mapper;

import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PatientMapperTest {

    private final PatientMapper mapper = new PatientMapperImpl();

    // --- toEntity ---

    @Test
    void toEntity_mapsAllRequestFields() {
        PatientRequest req = request("Anna", "Lindström");

        Patient entity = mapper.toEntity(req);

        assertThat(entity.getFirstName()).isEqualTo("Anna");
        assertThat(entity.getLastName()).isEqualTo("Lindström");
        assertThat(entity.getPersonalNumber()).isEqualTo("19850315-1234");
        assertThat(entity.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 3, 15));
        assertThat(entity.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(entity.getWard()).isEqualTo("ICU");
        assertThat(entity.getStatus()).isEqualTo(PatientStatus.ACTIVE);
    }

    @Test
    void toEntity_leavesAuditTimestampsNull() {
        Patient entity = mapper.toEntity(request("Anna", "Lindström"));

        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getUpdatedAt()).isNull();
    }

    // --- merge ---

    @Test
    void merge_copiesMutableFields() {
        Patient existing = existingPatient();
        PatientRequest update = new PatientRequest(
                "UpdatedFirst", "UpdatedLast", "19850315-1234",
                LocalDate.of(1985, 3, 15), Gender.MALE, "WARD-B", PatientStatus.DISCHARGED);

        mapper.merge(update, existing);

        assertThat(existing.getFirstName()).isEqualTo("UpdatedFirst");
        assertThat(existing.getLastName()).isEqualTo("UpdatedLast");
        assertThat(existing.getGender()).isEqualTo(Gender.MALE);
        assertThat(existing.getWard()).isEqualTo("WARD-B");
        assertThat(existing.getStatus()).isEqualTo(PatientStatus.DISCHARGED);
    }

    @Test
    void merge_doesNotOverwritePersonalNumber() {
        Patient existing = existingPatient();

        mapper.merge(new PatientRequest("X", "Y", "99991231-0000",
                LocalDate.of(2000, 1, 1), Gender.MALE, "WARD-B", PatientStatus.ACTIVE), existing);

        assertThat(existing.getPersonalNumber()).isEqualTo("19850315-1234");
    }

    @Test
    void merge_doesNotOverwriteDateOfBirth() {
        Patient existing = existingPatient();

        mapper.merge(new PatientRequest("X", "Y", "19850315-1234",
                LocalDate.of(2099, 12, 31), Gender.FEMALE, "ICU", PatientStatus.ACTIVE), existing);

        assertThat(existing.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 3, 15));
    }

    @Test
    void merge_doesNotOverwriteAuditTimestamps() {
        Patient existing = existingPatient();
        Instant originalCreatedAt = existing.getCreatedAt();
        Instant originalUpdatedAt = existing.getUpdatedAt();

        mapper.merge(request("UpdatedFirst", "UpdatedLast"), existing);

        assertThat(existing.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(existing.getUpdatedAt()).isEqualTo(originalUpdatedAt);
    }

    // --- helpers ---

    private PatientRequest request(String firstName, String lastName) {
        return new PatientRequest(firstName, lastName, "19850315-1234",
                LocalDate.of(1985, 3, 15), Gender.FEMALE, "ICU", PatientStatus.ACTIVE);
    }

    private Patient existingPatient() {
        Patient p = new Patient();
        p.setFirstName("Anna");
        p.setLastName("Lindström");
        p.setPersonalNumber("19850315-1234");
        p.setDateOfBirth(LocalDate.of(1985, 3, 15));
        p.setGender(Gender.FEMALE);
        p.setWard("ICU");
        p.setStatus(PatientStatus.ACTIVE);
        p.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));
        p.setUpdatedAt(Instant.parse("2024-06-01T00:00:00Z"));
        return p;
    }
}
