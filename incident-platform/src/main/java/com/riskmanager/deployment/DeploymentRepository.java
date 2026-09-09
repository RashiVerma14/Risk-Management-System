package com.riskmanager.deployment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DeploymentRepository extends MongoRepository<Deployment, String> {

    List<Deployment> findByServiceIdOrderByDeployedAtDesc(String serviceId, Pageable pageable);

    List<Deployment> findByServiceIdAndDeployedAtBetweenOrderByDeployedAtDesc(
            String serviceId, Instant from, Instant to);

    List<Deployment> findByDeployedAtBetweenOrderByDeployedAtDesc(Instant from, Instant to);
}
