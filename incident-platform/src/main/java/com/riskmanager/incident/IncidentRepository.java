package com.riskmanager.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends MongoRepository<Incident, String> {

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findBySeverity(Severity severity);

    List<Incident> findByAffectedServiceId(String affectedServiceId);

    List<Incident> findByAssignedEngineerId(String assignedEngineerId);

    Optional<Incident> findTopByCorrelationKeyAndStatusNotInOrderByCreatedAtDesc(
            String correlationKey, Collection<IncidentStatus> resolvedStatuses);

    long countByStatus(IncidentStatus status);

    long countBySeverity(Severity severity);

    long countByStatusIn(Collection<IncidentStatus> statuses);

    List<Incident> findByCreatedAtBetween(Instant from, Instant to);

    Page<Incident> findAll(Pageable pageable);
}
