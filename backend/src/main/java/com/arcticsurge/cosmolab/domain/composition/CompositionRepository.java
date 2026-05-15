package com.arcticsurge.cosmolab.domain.composition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CompositionRepository {
    Composition save(Composition composition);
    Optional<Composition> findById(UUID id);
    Page<Composition> findByEhrId(UUID ehrId, Pageable pageable);
    Page<Composition> findByEhrIdAndType(UUID ehrId, CompositionType type, Pageable pageable);
    boolean existsByIdAndEhrId(UUID id, UUID ehrId);
}
