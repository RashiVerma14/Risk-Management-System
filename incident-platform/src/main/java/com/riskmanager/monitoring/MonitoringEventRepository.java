package com.riskmanager.monitoring;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MonitoringEventRepository extends MongoRepository<MonitoringEvent, String> {

    List<MonitoringEvent> findByServiceIdOrderByTimestampDesc(String serviceId, Pageable pageable);

    List<MonitoringEvent> findByServiceIdAndEventTypeOrderByTimestampDesc(
            String serviceId, MonitoringEventType eventType, Pageable pageable);

    List<MonitoringEvent> findByTimestampBetween(Instant from, Instant to);

    long countByServiceIdAndEventType(String serviceId, MonitoringEventType eventType);
}
