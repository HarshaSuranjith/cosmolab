package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaPatientRepository extends
        JpaRepository<Patient, UUID>,
        JpaSpecificationExecutor<Patient>,
        PatientRepository {
}
