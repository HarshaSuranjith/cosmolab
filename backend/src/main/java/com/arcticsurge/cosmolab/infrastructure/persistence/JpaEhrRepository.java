package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.ehr.EhrRecord;
import com.arcticsurge.cosmolab.domain.ehr.EhrRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaEhrRepository extends JpaRepository<EhrRecord, UUID>, EhrRepository {

    Optional<EhrRecord> findBySubjectId(UUID subjectId);

    boolean existsBySubjectId(UUID subjectId);
}
