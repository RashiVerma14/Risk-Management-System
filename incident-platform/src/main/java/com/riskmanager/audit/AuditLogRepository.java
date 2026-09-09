package com.riskmanager.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findByResourceOrderByTimestampDesc(String resource, Pageable pageable);

    List<AuditLog> findByActorOrderByTimestampDesc(String actor, Pageable pageable);
}
