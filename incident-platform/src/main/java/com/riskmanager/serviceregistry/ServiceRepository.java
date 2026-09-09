package com.riskmanager.serviceregistry;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRepository extends MongoRepository<Service, String> {

    Optional<Service> findByName(String name);

    boolean existsByName(String name);

    List<Service> findByStatus(ServiceStatus status);

    List<Service> findByOwnerTeam(String ownerTeam);

    long countByStatus(ServiceStatus status);
}
