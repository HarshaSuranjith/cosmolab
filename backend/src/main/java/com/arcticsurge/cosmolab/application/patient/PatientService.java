package com.arcticsurge.cosmolab.application.patient;

import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientRepository;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.infrastructure.messaging.ClinicalEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final ClinicalEventPublisher eventPublisher;

    public Patient getById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    public Page<Patient> search(String ward, PatientStatus status, String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return patientRepository.findByNameContaining(search, pageable);
        }
        if (ward != null && status != null) {
            return patientRepository.findByWardAndStatus(ward, status, pageable);
        }
        if (ward != null) {
            return patientRepository.findByWard(ward, pageable);
        }
        if (status != null) {
            return patientRepository.findByStatus(status, pageable);
        }
        return patientRepository.findAll(pageable);
    }

    @Transactional
    public Patient create(Patient patient) {
        Patient saved = patientRepository.save(patient);
        eventPublisher.publishClinicalEvent("patient.created", saved.getId(), saved.getPersonalNumber());
        eventPublisher.publishAuditEvent(saved.getId(), "patient.created:" + saved.getPersonalNumber());
        return saved;
    }

    @Transactional
    public Patient update(UUID id, Patient updated) {
        Patient existing = getById(id);
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setWard(updated.getWard());
        existing.setStatus(updated.getStatus());
        existing.setGender(updated.getGender());
        Patient saved = patientRepository.save(existing);
        eventPublisher.publishClinicalEvent("patient.updated", saved.getId(), saved.getPersonalNumber());
        eventPublisher.publishAuditEvent(saved.getId(), "patient.updated:" + saved.getPersonalNumber());
        return saved;
    }

    @Transactional
    public void discharge(UUID id) {
        Patient patient = getById(id);
        patient.setStatus(PatientStatus.DISCHARGED);
        patientRepository.save(patient);
        eventPublisher.publishClinicalEvent("patient.discharged", patient.getId(), patient.getPersonalNumber());
        eventPublisher.publishAuditEvent(patient.getId(), "patient.discharged:" + patient.getPersonalNumber());
    }
}
