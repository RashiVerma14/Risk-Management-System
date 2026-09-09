package com.riskmanager.health;

import com.riskmanager.alert.AlertEngineService;
import com.riskmanager.incident.CreateIncidentRequest;
import com.riskmanager.incident.IncidentService;
import com.riskmanager.incident.Severity;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRepository;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import com.riskmanager.serviceregistry.ServiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "health.check.enabled", havingValue = "true", matchIfMissing = true)
public class ScheduledHealthChecker {

    private final ServiceRepository serviceRepository;
    private final ServiceRegistryService serviceRegistryService;
    private final HealthCheckRepository healthCheckRepository;
    private final IncidentService incidentService;

    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();

    private final RestClient restClient = createRestClient();

    private static RestClient createRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(2000));
        factory.setReadTimeout(Duration.ofMillis(2000));
        return RestClient.builder().requestFactory(factory).build();
    }

    @Scheduled(fixedDelayString = "${health.check.interval:60000}", initialDelay = 10000)
    public void runHealthChecks() {
        List<Service> services = serviceRepository.findAll();

        for (Service service : services) {
            if (service.getHealthEndpoint() != null && !service.getHealthEndpoint().isBlank()) {
                checkServiceHealth(service);
            }
        }
    }

    private void checkServiceHealth(Service service) {
        long start = System.currentTimeMillis();
        ServiceStatus newStatus = ServiceStatus.HEALTHY;
        int httpCode = 0;
        String errorMessage = null;

        try {
            var response = restClient.get()
                    .uri(service.getHealthEndpoint())
                    .retrieve()
                    .toBodilessEntity();

            long duration = System.currentTimeMillis() - start;
            httpCode = response.getStatusCode().value();

            if (httpCode >= 200 && httpCode < 300) {
                newStatus = (duration > 1500) ? ServiceStatus.DEGRADED : ServiceStatus.HEALTHY;
                consecutiveFailures.remove(service.getId());
            } else if (httpCode >= 500) {
                newStatus = ServiceStatus.DOWN;
                recordFailure(service, "HTTP " + httpCode);
            } else {
                newStatus = ServiceStatus.DEGRADED;
            }

            serviceRegistryService.updateServiceStatus(service.getId(), newStatus, duration);

        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            newStatus = ServiceStatus.DOWN;
            errorMessage = ex.getMessage();
            serviceRegistryService.updateServiceStatus(service.getId(), newStatus, duration);
            recordFailure(service, errorMessage);
        }

        healthCheckRepository.save(HealthCheckRecord.builder()
                .serviceId(service.getId())
                .serviceName(service.getName())
                .status(newStatus)
                .responseTimeMs(System.currentTimeMillis() - start)
                .httpStatusCode(httpCode > 0 ? httpCode : null)
                .errorMessage(errorMessage)
                .timestamp(Instant.now())
                .build());
    }

    private void recordFailure(Service service, String reason) {
        int failures = consecutiveFailures.merge(service.getId(), 1, Integer::sum);
        log.warn("Health check failed for service {} (consecutive: {}): {}", service.getName(), failures, reason);

        // If service fails 2 consecutive times, trigger an incident
        if (failures == 2) {
            String title = "Service Outage: " + service.getName() + " is UNREACHABLE";
            String description = String.format("Automated health check failed for %s at %s. Error: %s",
                    service.getName(), service.getHealthEndpoint(), reason);

            CreateIncidentRequest incReq = new CreateIncidentRequest(
                    title,
                    description,
                    Severity.CRITICAL,
                    service.getId(),
                    service.getId() + ":health_outage",
                    "Service health check timeout/failure",
                    List.of("health-check", "outage", service.getName()),
                    List.of(description)
            );

            try {
                incidentService.createIncident(incReq, "HEALTH_CHECKER");
                log.info("Triggered critical incident for service outage: {}", service.getName());
            } catch (Exception ex) {
                log.error("Failed to create outage incident: {}", ex.getMessage());
            }
        }
    }
}
