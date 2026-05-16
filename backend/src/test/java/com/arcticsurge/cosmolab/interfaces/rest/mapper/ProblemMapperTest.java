package com.arcticsurge.cosmolab.interfaces.rest.mapper;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemDiagnosis;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import com.arcticsurge.cosmolab.domain.evaluation.Severity;
import com.arcticsurge.cosmolab.interfaces.rest.dto.ProblemRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemMapperTest {

    private final ProblemMapper mapper = new ProblemMapperImpl();

    // --- merge ---

    @Test
    void merge_copiesDisplayName() {
        ProblemDiagnosis existing = existingEntry();

        mapper.merge(updateRequest("Updated diagnosis", Severity.SEVERE), existing);

        assertThat(existing.getDisplayName()).isEqualTo("Updated diagnosis");
    }

    @Test
    void merge_copiesSeverity() {
        ProblemDiagnosis existing = existingEntry();

        mapper.merge(updateRequest("Pneumonia", Severity.SEVERE), existing);

        assertThat(existing.getSeverity()).isEqualTo(Severity.SEVERE);
    }

    @Test
    void merge_doesNotOverwriteIcd10Code() {
        ProblemDiagnosis existing = existingEntry();

        mapper.merge(new ProblemRequest(UUID.randomUUID(), "Z99.9", "X", Severity.MILD,
                UUID.randomUUID(), LocalDate.now()), existing);

        assertThat(existing.getIcd10Code()).isEqualTo("J18.9");
    }

    @Test
    void merge_doesNotOverwriteEhrId() {
        ProblemDiagnosis existing = existingEntry();
        UUID originalEhrId = existing.getEhrId();

        mapper.merge(updateRequest("X", Severity.MILD), existing);

        assertThat(existing.getEhrId()).isEqualTo(originalEhrId);
    }

    @Test
    void merge_doesNotOverwriteCompositionId() {
        ProblemDiagnosis existing = existingEntry();
        UUID originalCompositionId = existing.getCompositionId();

        mapper.merge(updateRequest("X", Severity.MILD), existing);

        assertThat(existing.getCompositionId()).isEqualTo(originalCompositionId);
    }

    @Test
    void merge_doesNotOverwriteRecordedBy() {
        ProblemDiagnosis existing = existingEntry();
        UUID originalRecordedBy = existing.getRecordedBy();

        mapper.merge(updateRequest("X", Severity.MILD), existing);

        assertThat(existing.getRecordedBy()).isEqualTo(originalRecordedBy);
    }

    @Test
    void merge_doesNotOverwriteOnsetDate() {
        ProblemDiagnosis existing = existingEntry();
        LocalDate originalOnsetDate = existing.getOnsetDate();

        mapper.merge(updateRequest("X", Severity.MILD), existing);

        assertThat(existing.getOnsetDate()).isEqualTo(originalOnsetDate);
    }

    @Test
    void merge_doesNotOverwriteStatus() {
        ProblemDiagnosis existing = existingEntry();

        mapper.merge(updateRequest("X", Severity.MILD), existing);

        assertThat(existing.getStatus()).isEqualTo(ProblemStatus.ACTIVE);
    }

    @Test
    void merge_doesNotOverwriteRecordedAt() {
        ProblemDiagnosis existing = existingEntry();
        Instant originalRecordedAt = existing.getRecordedAt();

        mapper.merge(updateRequest("X", Severity.MILD), existing);

        assertThat(existing.getRecordedAt()).isEqualTo(originalRecordedAt);
    }

    // --- helpers ---

    private ProblemRequest updateRequest(String displayName, Severity severity) {
        return new ProblemRequest(UUID.randomUUID(), "J18.9", displayName, severity,
                UUID.randomUUID(), LocalDate.of(2024, 5, 1));
    }

    private ProblemDiagnosis existingEntry() {
        ProblemDiagnosis e = new ProblemDiagnosis();
        e.setEhrId(UUID.randomUUID());
        e.setCompositionId(UUID.randomUUID());
        e.setIcd10Code("J18.9");
        e.setDisplayName("Pneumonia");
        e.setSeverity(Severity.MODERATE);
        e.setStatus(ProblemStatus.ACTIVE);
        e.setRecordedBy(UUID.randomUUID());
        e.setOnsetDate(LocalDate.of(2024, 1, 15));
        e.setRecordedAt(Instant.parse("2024-01-15T10:00:00Z"));
        return e;
    }
}
