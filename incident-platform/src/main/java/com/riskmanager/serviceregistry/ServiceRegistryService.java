package com.riskmanager.serviceregistry;

import com.riskmanager.exception.BadRequestException;
import com.riskmanager.exception.ConflictException;
import com.riskmanager.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceRegistryService {

    private final ServiceRepository serviceRepository;

    public List<ServiceResponseDto> getAllServices() {
        return serviceRepository.findAll().stream()
                .map(ServiceResponseDto::from)
                .toList();
    }

    public ServiceResponseDto getServiceById(String id) {
        Service service = findServiceEntityById(id);
        return ServiceResponseDto.from(service);
    }

    public Service findServiceEntityById(String id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + id));
    }

    public ServiceResponseDto createService(CreateServiceRequest request) {
        if (serviceRepository.existsByName(request.name())) {
            throw new ConflictException("Service already registered with name: " + request.name());
        }

        Service service = Service.builder()
                .name(request.name())
                .description(request.description())
                .ownerTeam(request.ownerTeam())
                .environment(request.environment() != null ? request.environment() : "production")
                .healthEndpoint(request.healthEndpoint())
                .dependencies(request.dependencies() != null ? request.dependencies() : new ArrayList<>())
                .repositoryUrl(request.repositoryUrl())
                .version(request.version() != null ? request.version() : "1.0.0")
                .status(ServiceStatus.UNKNOWN)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Service saved = serviceRepository.save(service);
        log.info("Registered microservice: {} owned by {}", saved.getName(), saved.getOwnerTeam());
        return ServiceResponseDto.from(saved);
    }

    public ServiceResponseDto updateService(String id, UpdateServiceRequest request) {
        Service service = findServiceEntityById(id);

        if (!service.getName().equalsIgnoreCase(request.name()) && serviceRepository.existsByName(request.name())) {
            throw new ConflictException("Service name already taken: " + request.name());
        }

        service.setName(request.name());
        service.setDescription(request.description());
        service.setOwnerTeam(request.ownerTeam());
        if (request.environment() != null) service.setEnvironment(request.environment());
        service.setHealthEndpoint(request.healthEndpoint());
        if (request.dependencies() != null) service.setDependencies(request.dependencies());
        service.setRepositoryUrl(request.repositoryUrl());
        if (request.version() != null) service.setVersion(request.version());
        service.setUpdatedAt(Instant.now());

        Service updated = serviceRepository.save(service);
        log.info("Updated microservice details: {}", updated.getName());
        return ServiceResponseDto.from(updated);
    }

    public ServiceResponseDto updateServiceStatus(String id, ServiceStatus status, Long latencyMs) {
        Service service = findServiceEntityById(id);
        service.setStatus(status);
        service.setLastHealthCheckAt(Instant.now());
        if (latencyMs != null) {
            service.setResponseTimeMs(latencyMs);
        }
        service.setUpdatedAt(Instant.now());

        Service saved = serviceRepository.save(service);
        log.info("Updated service {} status to {} (latency: {}ms)", saved.getName(), status, latencyMs);
        return ServiceResponseDto.from(saved);
    }

    public ServiceDependencyDto getServiceDependencies(String id) {
        Service target = findServiceEntityById(id);

        List<ServiceResponseDto> upstream = new ArrayList<>();
        if (target.getDependencies() != null) {
            for (String depId : target.getDependencies()) {
                serviceRepository.findById(depId).ifPresent(s -> upstream.add(ServiceResponseDto.from(s)));
                serviceRepository.findByName(depId).ifPresent(s -> {
                    if (upstream.stream().noneMatch(u -> u.getId().equals(s.getId()))) {
                        upstream.add(ServiceResponseDto.from(s));
                    }
                });
            }
        }

        // Downstream: services that list this target id or target name in their dependencies
        List<ServiceResponseDto> downstream = serviceRepository.findAll().stream()
                .filter(s -> s.getDependencies() != null &&
                        (s.getDependencies().contains(target.getId()) || s.getDependencies().contains(target.getName())))
                .map(ServiceResponseDto::from)
                .toList();

        return ServiceDependencyDto.builder()
                .service(ServiceResponseDto.from(target))
                .upstreamDependencies(upstream)
                .downstreamDependents(downstream)
                .build();
    }

    public void deleteService(String id) {
        Service service = findServiceEntityById(id);
        serviceRepository.delete(service);
        log.info("Deleted microservice: {}", service.getName());
    }
}
