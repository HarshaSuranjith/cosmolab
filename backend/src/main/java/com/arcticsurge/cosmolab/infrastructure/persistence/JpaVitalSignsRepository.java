package com.arcticsurge.cosmolab.infrastructure.persistence;

import com.arcticsurge.cosmolab.domain.observation.VitalSigns;
import com.arcticsurge.cosmolab.domain.observation.VitalSignsRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaVitalSignsRepository extends JpaRepository<VitalSigns, UUID>, VitalSignsRepository {

    List<VitalSigns> findByCompositionId(UUID compositionId);

    @Query("""
            SELECT v FROM VitalSigns v
            JOIN Composition c ON v.compositionId = c.id
            WHERE c.ehrId = :ehrId AND v.recordedAt BETWEEN :from AND :to
            ORDER BY v.recordedAt DESC
            """)
    List<VitalSigns> findByEhrIdBetween(@Param("ehrId") UUID ehrId,
                                         @Param("from") Instant from,
                                         @Param("to") Instant to);

    @Query("""
            SELECT v FROM VitalSigns v
            JOIN Composition c ON v.compositionId = c.id
            WHERE c.ehrId = :ehrId
            ORDER BY v.recordedAt DESC
            LIMIT 1
            """)
    Optional<VitalSigns> findLatestByEhrId(@Param("ehrId") UUID ehrId);
}
