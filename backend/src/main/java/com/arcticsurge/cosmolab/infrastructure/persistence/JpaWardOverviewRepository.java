package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.patient.PatientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaWardOverviewRepository extends JpaRepository<WardOverviewRecord, UUID> {

    List<WardOverviewRecord> findByWardAndPatientStatus(String ward, PatientStatus patientStatus);
}
