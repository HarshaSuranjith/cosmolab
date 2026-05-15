package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.composition.Composition;
import com.arcticsurge.cosmolab.domain.composition.CompositionRepository;
import com.arcticsurge.cosmolab.domain.composition.CompositionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaCompositionRepository extends JpaRepository<Composition, UUID>, CompositionRepository {

    Page<Composition> findByEhrId(UUID ehrId, Pageable pageable);

    Page<Composition> findByEhrIdAndType(UUID ehrId, CompositionType type, Pageable pageable);

    boolean existsByIdAndEhrId(UUID id, UUID ehrId);
}
