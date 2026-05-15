package com.arcticsurge.cosmolab.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditEventRecordRepository extends JpaRepository<AuditEventRecord, UUID> {
    boolean existsByEventId(String eventId);
}
