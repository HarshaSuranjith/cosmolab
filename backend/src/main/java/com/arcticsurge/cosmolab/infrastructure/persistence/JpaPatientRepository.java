package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.patient.Patient;
import com.arcticsurge.cosmolab.domain.patient.PatientRepository;
import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaPatientRepository extends JpaRepository<Patient, UUID>, PatientRepository {

    Page<Patient> findByWardAndStatus(String ward, PatientStatus status, Pageable pageable);

    Page<Patient> findByWard(String ward, Pageable pageable);

    Page<Patient> findByStatus(PatientStatus status, Pageable pageable);

    @Query("SELECT p FROM Patient p WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Patient> findByNameContaining(@Param("search") String search, Pageable pageable);
}
