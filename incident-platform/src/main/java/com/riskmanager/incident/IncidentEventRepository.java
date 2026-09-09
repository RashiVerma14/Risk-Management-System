package com.riskmanager.incident;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentEventRepository extends MongoRepository<IncidentEvent, String> {

    List<IncidentEvent> findByIncidentIdOrderByTimestampAsc(String incidentId);

    void deleteByIncidentId(String incidentId);
}
