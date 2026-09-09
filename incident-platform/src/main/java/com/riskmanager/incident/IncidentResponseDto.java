package com.riskmanager.incident;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponseDto {

    private String id;
    private String title;
    private String description;
    private Severity severity;
    private IncidentStatus status;
    private String affectedServiceId;
    private String affectedServiceName;
    private String assignedEngineerId;
    private String assignedEngineerName;
    private String correlationKey;
    private int eventCount;
    private String impact;
    private String rootCause;
    private String resolution;
    private List<String> tags;
    private List<String> logs;
    private Instant detectedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant acknowledgedAt;
    private Instant mitigatedAt;
    private Instant resolvedAt;
    private Instant closedAt;

    public static IncidentResponseDto from(Incident incident, String serviceName, String engineerName) {
        return IncidentResponseDto.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .affectedServiceId(incident.getAffectedServiceId())
                .affectedServiceName(serviceName)
                .assignedEngineerId(incident.getAssignedEngineerId())
                .assignedEngineerName(engineerName)
                .correlationKey(incident.getCorrelationKey())
                .eventCount(incident.getEventCount())
                .impact(incident.getImpact())
                .rootCause(incident.getRootCause())
                .resolution(incident.getResolution())
                .tags(incident.getTags())
                .logs(incident.getLogs())
                .detectedAt(incident.getDetectedAt())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .acknowledgedAt(incident.getAcknowledgedAt())
                .mitigatedAt(incident.getMitigatedAt())
                .resolvedAt(incident.getResolvedAt())
                .closedAt(incident.getClosedAt())
                .build();
    }
}
