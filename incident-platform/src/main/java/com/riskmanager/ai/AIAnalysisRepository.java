package com.riskmanager.ai;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AIAnalysisRepository extends MongoRepository<AIAnalysis, String> {

    List<AIAnalysis> findByIncidentIdOrderByCreatedAtDesc(String incidentId);

    Optional<AIAnalysis> findTopByIncidentIdOrderByCreatedAtDesc(String incidentId);

    long count();
}
