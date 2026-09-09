package com.riskmanager.health;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HealthCheckRepository extends MongoRepository<HealthCheckRecord, String> {

    List<HealthCheckRecord> findByServiceIdOrderByTimestampDesc(String serviceId, Pageable pageable);
}
