package com.arcticsurge.cosmolab.domain.patient;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository {
    Patient save(Patient patient);
    Optional<Patient> findById(UUID id);
    Page<Patient> findAll(Specification<Patient> spec, Pageable pageable);
    void deleteById(UUID id);
    boolean existsById(UUID id);
}
