package com.riskmanager.alert;

import com.riskmanager.incident.Severity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlertRepository extends MongoRepository<Alert, String> {

    List<Alert> findByServiceIdOrderByTimestampDesc(String serviceId, Pageable pageable);

    List<Alert> findByStatusOrderByTimestampDesc(AlertStatus status);

    List<Alert> findBySeverityOrderByTimestampDesc(Severity severity);

    long countByStatus(AlertStatus status);

    long countBySeverity(Severity severity);

    List<Alert> findByTimestampBetween(Instant from, Instant to);
}
