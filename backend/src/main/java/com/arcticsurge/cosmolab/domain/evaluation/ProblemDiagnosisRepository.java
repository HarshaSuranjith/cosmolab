package com.arcticsurge.cosmolab.domain.evaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemDiagnosisRepository {
    ProblemDiagnosis save(ProblemDiagnosis entry);
    Optional<ProblemDiagnosis> findById(UUID id);
    List<ProblemDiagnosis> findByEhrId(UUID ehrId);
    List<ProblemDiagnosis> findByEhrIdAndStatus(UUID ehrId, ProblemStatus status);
    long countByEhrIdAndStatus(UUID ehrId, ProblemStatus status);
}
