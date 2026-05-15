package com.arcticsurge.cosmolab.domain.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository {
    Patient save(Patient patient);
    Optional<Patient> findById(UUID id);
    Page<Patient> findAll(Pageable pageable);
    Page<Patient> findByWardAndStatus(String ward, PatientStatus status, Pageable pageable);
    Page<Patient> findByWard(String ward, Pageable pageable);
    Page<Patient> findByStatus(PatientStatus status, Pageable pageable);
    Page<Patient> findByNameContaining(String search, Pageable pageable);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
