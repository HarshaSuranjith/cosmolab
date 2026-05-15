package com.arcticsurge.cosmolab.application.patient;

import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientRepository;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import com.arcticsurge.cosmolab.infrastructure.messaging.ClinicalEventPublisher;
import com.arcticsurge.cosmolab.infrastructure.persistence.PatientSpecifications;
import com.arcticsurge.cosmolab.interfaces.rest.dto.PatientRequest;
import com.arcticsurge.cosmolab.interfaces.rest.mapper.PatientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final ClinicalEventPublisher eventPublisher;
    private final PatientMapper patientMapper;

    public Patient getById(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    public Page<Patient> search(String ward, PatientStatus status, String search, Pageable pageable) {
        Specification<Patient> spec = Specification
                .where(PatientSpecifications.wardEquals(ward))
                .and(PatientSpecifications.statusEquals(status))
                .and(PatientSpecifications.nameContains(search));
        return patientRepository.findAll(spec, pageable);
    }

    @Transactional
    public Patient create(Patient patient) {
        Patient saved = patientRepository.save(patient);
        eventPublisher.publishClinicalEvent("patient.created", saved.getId(), saved.getPersonalNumber());
        eventPublisher.publishAuditEvent(saved.getId(), "patient.created:" + saved.getPersonalNumber());
        return saved;
    }

    @Transactional
    public Patient update(UUID id, PatientRequest request) {
        Patient existing = getById(id);
        patientMapper.merge(request, existing);
        eventPublisher.publishClinicalEvent("patient.updated", existing.getId(), existing.getPersonalNumber());
        eventPublisher.publishAuditEvent(existing.getId(), "patient.updated:" + existing.getPersonalNumber());
        return existing;
    }

    @Transactional
    public void discharge(UUID id) {
        Patient patient = getById(id);
        patient.setStatus(PatientStatus.DISCHARGED);
        eventPublisher.publishClinicalEvent("patient.discharged", patient.getId(), patient.getPersonalNumber());
        eventPublisher.publishAuditEvent(patient.getId(), "patient.discharged:" + patient.getPersonalNumber());
    }
}
