package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.evaluation.ProblemListEntry;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemListRepository;
import com.arcticsurge.cosmolab.domain.evaluation.ProblemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaProblemListRepository extends JpaRepository<ProblemListEntry, UUID>, ProblemListRepository {

    List<ProblemListEntry> findByEhrId(UUID ehrId);

    List<ProblemListEntry> findByEhrIdAndStatus(UUID ehrId, ProblemStatus status);

    long countByEhrIdAndStatus(UUID ehrId, ProblemStatus status);
}
