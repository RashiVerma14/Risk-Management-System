package com.riskmanager.dashboard;

import com.riskmanager.ai.AIAnalysisRepository;
import com.riskmanager.alert.AlertRepository;
import com.riskmanager.alert.AlertStatus;
import com.riskmanager.incident.Incident;
import com.riskmanager.incident.IncidentRepository;
import com.riskmanager.incident.IncidentStatus;
import com.riskmanager.incident.Severity;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRepository;
import com.riskmanager.serviceregistry.ServiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class DashboardService {

    private final IncidentRepository incidentRepository;
    private final ServiceRepository serviceRepository;
    private final AlertRepository alertRepository;
    private final AIAnalysisRepository aiAnalysisRepository;

    public DashboardMetricsDto getDashboardMetrics(String window) {
        Duration duration = switch (window != null ? window.toLowerCase() : "24h") {
            case "7d" -> Duration.ofDays(7);
            case "30d" -> Duration.ofDays(30);
            default -> Duration.ofHours(24);
        };

        Instant from = Instant.now().minus(duration);
        Instant to = Instant.now();

        List<Incident> incidents = incidentRepository.findByCreatedAtBetween(from, to);
        if (incidents.isEmpty()) {
            incidents = incidentRepository.findAll();
        }

        List<Service> services = serviceRepository.findAll();

        long totalIncidents = incidents.size();
        long openIncidents = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.OPEN).count();
        long criticalIncidents = incidents.stream().filter(i -> i.getSeverity() == Severity.CRITICAL).count();

        // Calculate MTTR (Mean Time To Resolve in minutes)
        List<Double> mttrValues = new ArrayList<>();
        List<Double> mttaValues = new ArrayList<>();

        for (Incident incident : incidents) {
            if (incident.getResolvedAt() != null && incident.getCreatedAt() != null) {
                long minutes = Duration.between(incident.getCreatedAt(), incident.getResolvedAt()).toMinutes();
                if (minutes >= 0) mttrValues.add((double) minutes);
            }
            if (incident.getAcknowledgedAt() != null && incident.getCreatedAt() != null) {
                long minutes = Duration.between(incident.getCreatedAt(), incident.getAcknowledgedAt()).toMinutes();
                if (minutes >= 0) mttaValues.add((double) minutes);
            }
        }

        double avgMttr = mttrValues.isEmpty() ? 0.0 : mttrValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double avgMtta = mttaValues.isEmpty() ? 0.0 : mttaValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        // Service health availability
        long healthyCount = services.stream().filter(s -> s.getStatus() == ServiceStatus.HEALTHY).count();
        double availability = services.isEmpty() ? 100.0 : ((double) healthyCount / services.size()) * 100.0;

        // Breakdowns
        Map<Severity, Long> bySeverity = incidents.stream()
                .collect(Collectors.groupingBy(Incident::getSeverity, Collectors.counting()));

        Map<IncidentStatus, Long> byStatus = incidents.stream()
                .collect(Collectors.groupingBy(Incident::getStatus, Collectors.counting()));

        Map<String, Long> byService = incidents.stream()
                .collect(Collectors.groupingBy(Incident::getAffectedServiceId, Collectors.counting()));

        Map<ServiceStatus, Long> servicesByStatus = services.stream()
                .collect(Collectors.groupingBy(Service::getStatus, Collectors.counting()));

        long totalAlerts = alertRepository.count();
        long activeAlerts = alertRepository.countByStatus(AlertStatus.ACTIVE);
        long totalAiAnalyses = aiAnalysisRepository.count();

        return DashboardMetricsDto.builder()
                .totalIncidents(totalIncidents)
                .openIncidents(openIncidents)
                .criticalIncidents(criticalIncidents)
                .averageMttrMinutes(Math.round(avgMttr * 10.0) / 10.0)
                .averageMttaMinutes(Math.round(avgMtta * 10.0) / 10.0)
                .serviceAvailabilityPercentage(Math.round(availability * 10.0) / 10.0)
                .incidentsBySeverity(bySeverity)
                .incidentsByStatus(byStatus)
                .incidentsByService(byService)
                .servicesByStatus(servicesByStatus)
                .totalAlerts(totalAlerts)
                .activeAlerts(activeAlerts)
                .totalAiAnalyses(totalAiAnalyses)
                .timeWindow(window != null ? window : "24h")
                .build();
    }
}
