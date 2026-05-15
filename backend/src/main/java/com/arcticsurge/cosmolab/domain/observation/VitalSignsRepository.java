package com.arcticsurge.cosmolab.domain.observation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VitalSignsRepository {
    VitalSigns save(VitalSigns vitalSigns);
    Optional<VitalSigns> findById(UUID id);
    List<VitalSigns> findByCompositionId(UUID compositionId);
    List<VitalSigns> findByEhrIdBetween(UUID ehrId, Instant from, Instant to);
    Optional<VitalSigns> findLatestByEhrId(UUID ehrId);
}
