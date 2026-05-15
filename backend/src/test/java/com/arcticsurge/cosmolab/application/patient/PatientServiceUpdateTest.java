package com.arcticsurge.cosmolab.application.patient;

import com.arcticsurge.cosmolab.domain.patient.Gender;
import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientRepository;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.infrastructure.messaging.ClinicalEventPublisher;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import com.arcticsurge.cosmolab.interfaces.rest.mapper.PatientMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceUpdateTest {

    @Mock PatientRepository patientRepository;
    @Mock ClinicalEventPublisher eventPublisher;
    @Mock PatientMapper patientMapper;
    @InjectMocks PatientService patientService;

    private final UUID patientId = UUID.randomUUID();

    private Patient stubPatient() {
        Patient p = new Patient();
        p.setFirstName("Anna");
        p.setLastName("Lindström");
        p.setPersonalNumber("19850315-1234");
        p.setWard("ICU");
        p.setStatus(PatientStatus.ACTIVE);
        return p;
    }

    private PatientRequest updateRequest() {
        return new PatientRequest("Updated", "Name", "19850315-1234",
                LocalDate.of(1985, 3, 15), Gender.FEMALE, "WARD-B", PatientStatus.ACTIVE);
    }

    // --- update ---

    @Test
    void update_delegatesToMapper_notManualSetters() {
        Patient existing = stubPatient();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existing));
        PatientRequest request = updateRequest();

        patientService.update(patientId, request);

        verify(patientMapper).merge(request, existing);
    }

    @Test
    void update_doesNotCallSave_dirtyCheckingHandlesFlush() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(stubPatient()));

        patientService.update(patientId, updateRequest());

        verify(patientRepository, never()).save(any());
    }

    @Test
    void update_publishesClinicalAndAuditEvents() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(stubPatient()));

        patientService.update(patientId, updateRequest());

        verify(eventPublisher).publishClinicalEvent(eq("patient.updated"), any(), any());
        verify(eventPublisher).publishAuditEvent(any(), contains("patient.updated"));
    }

    @Test
    void update_throwsPatientNotFoundException_whenPatientMissing() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.update(patientId, updateRequest()))
                .isInstanceOf(PatientNotFoundException.class)
                .hasMessageContaining(patientId.toString());
    }

    // --- discharge ---

    @Test
    void discharge_setsStatusToDischargedDirectly() {
        Patient existing = stubPatient();
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(existing));

        patientService.discharge(patientId);

        assertThat(existing.getStatus()).isEqualTo(PatientStatus.DISCHARGED);
    }

    @Test
    void discharge_doesNotCallSave_dirtyCheckingHandlesFlush() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(stubPatient()));

        patientService.discharge(patientId);

        verify(patientRepository, never()).save(any());
    }

    @Test
    void discharge_publishesClinicalAndAuditEvents() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(stubPatient()));

        patientService.discharge(patientId);

        verify(eventPublisher).publishClinicalEvent(eq("patient.discharged"), any(), any());
        verify(eventPublisher).publishAuditEvent(any(), contains("patient.discharged"));
    }
}
