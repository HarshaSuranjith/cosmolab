package com.arcticsurge.cosmolab.domain.evaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemListRepository {
    ProblemListEntry save(ProblemListEntry entry);
    Optional<ProblemListEntry> findById(UUID id);
    List<ProblemListEntry> findByEhrId(UUID ehrId);
    List<ProblemListEntry> findByEhrIdAndStatus(UUID ehrId, ProblemStatus status);
    long countByEhrIdAndStatus(UUID ehrId, ProblemStatus status);
}
