package com.riskmanager.deployment;

import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final ServiceRegistryService serviceRegistryService;

    public Deployment recordDeployment(DeploymentRequest request) {
        String serviceName = request.serviceId();
        try {
            Service s = serviceRegistryService.findServiceEntityById(request.serviceId());
            serviceName = s.getName();
        } catch (Exception ignored) {}

        Deployment deployment = Deployment.builder()
                .serviceId(request.serviceId())
                .serviceName(serviceName)
                .version(request.version())
                .environment(request.environment() != null ? request.environment() : "production")
                .deployedAt(Instant.now())
                .deployedBy(request.deployedBy() != null ? request.deployedBy() : "CI/CD Pipeline")
                .commitHash(request.commitHash())
                .status(request.status() != null ? request.status() : "SUCCESS")
                .changelog(request.changelog())
                .build();

        Deployment saved = deploymentRepository.save(deployment);
        log.info("Recorded deployment of service {} version {}", serviceName, request.version());
        return saved;
    }

    public List<Deployment> getRecentDeployments(String serviceId, int limit) {
        return deploymentRepository.findByServiceIdOrderByDeployedAtDesc(serviceId, PageRequest.of(0, limit));
    }

    public List<Deployment> getAllRecentDeployments(int limit) {
        Instant pastWindow = Instant.now().minus(Duration.ofDays(7));
        return deploymentRepository.findByDeployedAtBetweenOrderByDeployedAtDesc(pastWindow, Instant.now());
    }

    /**
     * Correlates an incident with recent deployments within a time window (e.g. 30 minutes prior to incident detectedAt).
     */
    public List<Deployment> findCorrelatedDeployments(String serviceId, Instant detectedAt, Duration lookback) {
        Instant from = detectedAt.minus(lookback);
        Instant to = detectedAt.plus(Duration.ofMinutes(5));

        List<Deployment> directDeployments = deploymentRepository
                .findByServiceIdAndDeployedAtBetweenOrderByDeployedAtDesc(serviceId, from, to);

        List<Deployment> results = new ArrayList<>(directDeployments);

        // Also check upstream dependencies for recent deployments
        try {
            Service service = serviceRegistryService.findServiceEntityById(serviceId);
            if (service.getDependencies() != null) {
                for (String depId : service.getDependencies()) {
                    List<Deployment> depDeployments = deploymentRepository
                            .findByServiceIdAndDeployedAtBetweenOrderByDeployedAtDesc(depId, from, to);
                    results.addAll(depDeployments);
                }
            }
        } catch (Exception ignored) {}

        return results;
    }
}
