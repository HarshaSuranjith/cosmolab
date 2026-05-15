package com.arcticsurge.cosmolab.interfaces.rest.mapper;

import com.arcticsurge.cosmolab.domain.composition.Composition;
import com.arcticsurge.cosmolab.domain.composition.CompositionStatus;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import com.arcticsurge.cosmolab.interfaces.rest.dto.CompositionRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompositionMapperTest {

    private final CompositionMapper mapper = new CompositionMapperImpl();

    // --- merge ---

    @Test
    void merge_copiesType_startTime_facilityName() {
        Composition existing = existingComposition();
        Instant newStartTime = Instant.parse("2025-06-01T08:30:00Z");
        CompositionRequest update = new CompositionRequest(
                CompositionType.DISCHARGE_SUMMARY, UUID.randomUUID(), newStartTime, "New Facility");

        mapper.merge(update, existing);

        assertThat(existing.getType()).isEqualTo(CompositionType.DISCHARGE_SUMMARY);
        assertThat(existing.getStartTime()).isEqualTo(newStartTime);
        assertThat(existing.getFacilityName()).isEqualTo("New Facility");
    }

    @Test
    void merge_doesNotOverwriteEhrId() {
        Composition existing = existingComposition();
        UUID originalEhrId = existing.getEhrId();

        mapper.merge(new CompositionRequest(
                CompositionType.PROGRESS_NOTE, UUID.randomUUID(), Instant.now(), "X"), existing);

        assertThat(existing.getEhrId()).isEqualTo(originalEhrId);
    }

    @Test
    void merge_doesNotOverwriteAuthorId() {
        Composition existing = existingComposition();
        UUID originalAuthorId = existing.getAuthorId();

        mapper.merge(new CompositionRequest(
                CompositionType.PROGRESS_NOTE, UUID.randomUUID(), Instant.now(), "X"), existing);

        assertThat(existing.getAuthorId()).isEqualTo(originalAuthorId);
    }

    @Test
    void merge_doesNotOverwriteCommitTime() {
        Composition existing = existingComposition();
        Instant originalCommitTime = existing.getCommitTime();

        mapper.merge(new CompositionRequest(
                CompositionType.DISCHARGE_SUMMARY, UUID.randomUUID(), Instant.now(), "X"), existing);

        assertThat(existing.getCommitTime()).isEqualTo(originalCommitTime);
    }

    @Test
    void merge_doesNotOverwriteStatus() {
        Composition existing = existingComposition();
        CompositionStatus originalStatus = existing.getStatus();

        mapper.merge(new CompositionRequest(
                CompositionType.DISCHARGE_SUMMARY, UUID.randomUUID(), Instant.now(), "X"), existing);

        assertThat(existing.getStatus()).isEqualTo(originalStatus);
    }

    // --- helpers ---

    private Composition existingComposition() {
        Composition c = new Composition();
        c.setEhrId(UUID.randomUUID());
        c.setType(CompositionType.ENCOUNTER_NOTE);
        c.setAuthorId(UUID.randomUUID());
        c.setStartTime(Instant.parse("2024-01-01T08:00:00Z"));
        c.setCommitTime(Instant.parse("2024-01-01T08:00:00Z"));
        c.setFacilityName("Original Facility");
        c.setStatus(CompositionStatus.COMPLETE);
        return c;
    }
}
