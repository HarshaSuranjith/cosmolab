package com.arcticsurge.cosmolab.domain;

import com.arcticsurge.cosmolab.application.composition.CompositionNotFoundException;
import com.arcticsurge.cosmolab.application.ehr.EhrNotFoundException;
import com.arcticsurge.cosmolab.application.evaluation.ProblemListEntryNotFoundException;
import com.arcticsurge.cosmolab.application.patient.PatientNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityNotFoundExceptionHierarchyTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void patientNotFoundException_extendsEntityNotFoundException() {
        assertThat(new PatientNotFoundException(ID)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void ehrNotFoundException_extendsEntityNotFoundException() {
        assertThat(new EhrNotFoundException(ID)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void compositionNotFoundException_extendsEntityNotFoundException() {
        assertThat(new CompositionNotFoundException(ID)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void problemListEntryNotFoundException_extendsEntityNotFoundException() {
        assertThat(new ProblemListEntryNotFoundException(ID)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void patientNotFoundException_messageContainsId() {
        assertThat(new PatientNotFoundException(ID).getMessage())
                .isEqualTo("Patient 00000000-0000-0000-0000-000000000001 not found");
    }

    @Test
    void ehrNotFoundException_messageContainsId() {
        assertThat(new EhrNotFoundException(ID).getMessage())
                .isEqualTo("EHR 00000000-0000-0000-0000-000000000001 not found");
    }

    @Test
    void compositionNotFoundException_messageContainsId() {
        assertThat(new CompositionNotFoundException(ID).getMessage())
                .isEqualTo("Composition 00000000-0000-0000-0000-000000000001 not found");
    }

    @Test
    void problemListEntryNotFoundException_messageContainsId() {
        assertThat(new ProblemListEntryNotFoundException(ID).getMessage())
                .isEqualTo("Problem list entry 00000000-0000-0000-0000-000000000001 not found");
    }
}
