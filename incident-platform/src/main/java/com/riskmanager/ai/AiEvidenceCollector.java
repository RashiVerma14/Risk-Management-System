package com.riskmanager.ai;

import com.riskmanager.alert.Alert;
import com.riskmanager.alert.AlertRepository;
import com.riskmanager.deployment.Deployment;
import com.riskmanager.deployment.DeploymentService;
import com.riskmanager.incident.Incident;
import com.riskmanager.incident.IncidentRepository;
import com.riskmanager.incident.IncidentStatus;
import com.riskmanager.monitoring.MonitoringEvent;
import com.riskmanager.monitoring.MonitoringEventRepository;
import com.riskmanager.monitoring.MonitoringEventType;
import com.riskmanager.rag.KnowledgeDocument;
import com.riskmanager.rag.RagService;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceDependencyDto;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiEvidenceCollector {

    private final ServiceRegistryService serviceRegistryService;
    private final MonitoringEventRepository monitoringEventRepository;
    private final AlertRepository alertRepository;
    private final DeploymentService deploymentService;
    private final RagService ragService;
    private final IncidentRepository incidentRepository;

    public AiInvestigationContext collectEvidence(Incident incident) {
        String serviceId = incident.getAffectedServiceId();

        Service service = null;
        ServiceDependencyDto depGraph = null;
        try {
            service = serviceRegistryService.findServiceEntityById(serviceId);
            depGraph = serviceRegistryService.getServiceDependencies(serviceId);
        } catch (Exception ex) {
            log.warn("Failed to load service or dependencies for serviceId {}: {}", serviceId, ex.getMessage());
        }

        // Recent error and warning logs
        List<MonitoringEvent> recentLogs = monitoringEventRepository
                .findByServiceIdAndEventTypeOrderByTimestampDesc(serviceId, MonitoringEventType.LOG, PageRequest.of(0, 30));

        // Recent alerts
        List<Alert> recentAlerts = alertRepository
                .findByServiceIdOrderByTimestampDesc(serviceId, PageRequest.of(0, 10));

        // Recent correlated deployments (lookback 60 minutes)
        List<Deployment> deployments = deploymentService
                .findCorrelatedDeployments(serviceId, incident.getDetectedAt(), Duration.ofMinutes(60));

        // RAG Runbooks and postmortems
        String searchQuery = incident.getTitle() + " " + (incident.getDescription() != null ? incident.getDescription() : "");
        List<KnowledgeDocument> runbooks = ragService.retrieveRelevantDocuments(searchQuery, serviceId, 3);

        // Historical resolved incidents for the same service
        List<Incident> historical = incidentRepository.findByAffectedServiceId(serviceId).stream()
                .filter(i -> !i.getId().equals(incident.getId()))
                .filter(i -> i.getStatus() == IncidentStatus.RESOLVED || i.getStatus() == IncidentStatus.CLOSED)
                .limit(5)
                .toList();

        return AiInvestigationContext.builder()
                .incident(incident)
                .affectedService(service)
                .dependencyGraph(depGraph)
                .recentLogs(recentLogs)
                .recentAlerts(recentAlerts)
                .recentDeployments(deployments)
                .retrievedRunbooks(runbooks)
                .historicalIncidents(historical)
                .build();
    }
}
