package com.arcticsurge.cosmolab.domain.ehr;

import java.util.Optional;
import java.util.UUID;

public interface EhrRepository {
    EhrRecord save(EhrRecord ehr);
    Optional<EhrRecord> findById(UUID ehrId);
    Optional<EhrRecord> findBySubjectId(UUID patientId);
    boolean existsBySubjectId(UUID patientId);
}
