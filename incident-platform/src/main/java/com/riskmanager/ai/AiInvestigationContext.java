package com.riskmanager.ai;

import com.riskmanager.alert.Alert;
import com.riskmanager.deployment.Deployment;
import com.riskmanager.incident.Incident;
import com.riskmanager.monitoring.MonitoringEvent;
import com.riskmanager.rag.KnowledgeDocument;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceDependencyDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiInvestigationContext {

    private Incident incident;
    private Service affectedService;
    private ServiceDependencyDto dependencyGraph;
    @Builder.Default
    private List<MonitoringEvent> recentLogs = new ArrayList<>();
    @Builder.Default
    private List<Alert> recentAlerts = new ArrayList<>();
    @Builder.Default
    private List<Deployment> recentDeployments = new ArrayList<>();
    @Builder.Default
    private List<KnowledgeDocument> retrievedRunbooks = new ArrayList<>();
    @Builder.Default
    private List<Incident> historicalIncidents = new ArrayList<>();
}
