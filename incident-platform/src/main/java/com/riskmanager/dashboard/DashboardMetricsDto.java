package com.riskmanager.dashboard;

import com.riskmanager.incident.IncidentStatus;
import com.riskmanager.incident.Severity;
import com.riskmanager.serviceregistry.ServiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsDto {

    private long totalIncidents;
    private long openIncidents;
    private long criticalIncidents;
    private double averageMttrMinutes;
    private double averageMttaMinutes;
    private double serviceAvailabilityPercentage;

    private Map<Severity, Long> incidentsBySeverity;
    private Map<IncidentStatus, Long> incidentsByStatus;
    private Map<String, Long> incidentsByService;
    private Map<ServiceStatus, Long> servicesByStatus;

    private long totalAlerts;
    private long activeAlerts;
    private long totalAiAnalyses;
    private String timeWindow;
}
