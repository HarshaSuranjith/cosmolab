package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemDiagnosis;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemDiagnosisRepository;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaProblemDiagnosisRepository extends JpaRepository<ProblemDiagnosis, UUID>, ProblemDiagnosisRepository {

    List<ProblemDiagnosis> findByEhrId(UUID ehrId);

    List<ProblemDiagnosis> findByEhrIdAndStatus(UUID ehrId, ProblemStatus status);

    long countByEhrIdAndStatus(UUID ehrId, ProblemStatus status);
}
