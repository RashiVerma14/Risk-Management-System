package com.riskmanager.monitoring;

import com.riskmanager.alert.AlertEngineService;
import com.riskmanager.incident.CreateIncidentRequest;
import com.riskmanager.incident.IncidentService;
import com.riskmanager.incident.Severity;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import com.riskmanager.serviceregistry.ServiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class MonitoringService {

    private final MonitoringEventRepository monitoringEventRepository;
    private final AlertEngineService alertEngineService;
    private final ServiceRegistryService serviceRegistryService;
    private final IncidentService incidentService;

    public MonitoringEvent ingestLog(LogIngestionRequest request) {
        Map<String, Object> payload = new HashMap<>();
        if (request.context() != null) payload.putAll(request.context());
        if (request.logger() != null) payload.put("logger", request.logger());
        if (request.traceId() != null) payload.put("traceId", request.traceId());

        MonitoringEvent event = MonitoringEvent.builder()
                .serviceId(request.serviceId())
                .eventType(MonitoringEventType.LOG)
                .severity(request.level())
                .source("LOG_INGESTION")
                .message(request.message())
                .environment(request.environment() != null ? request.environment() : "production")
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        MonitoringEvent saved = monitoringEventRepository.save(event);
        log.debug("Ingested log event for service {}: {}", request.serviceId(), request.message());

        // Evaluate for alert escalation if error/critical
        alertEngineService.evaluateLogEvent(request.serviceId(), request.level(), request.message());

        return saved;
    }

    public MonitoringEvent ingestMetric(MetricIngestionRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("metricName", request.metricName());
        payload.put("value", request.value());
        payload.put("unit", request.unit());
        if (request.tags() != null) payload.put("tags", request.tags());

        MonitoringEvent event = MonitoringEvent.builder()
                .serviceId(request.serviceId())
                .eventType(MonitoringEventType.METRIC)
                .source("METRIC_INGESTION")
                .message(String.format("Metric %s = %s %s", request.metricName(), request.value(), request.unit() != null ? request.unit() : ""))
                .environment(request.environment() != null ? request.environment() : "production")
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        MonitoringEvent saved = monitoringEventRepository.save(event);

        // Evaluate metric against alert rules
        alertEngineService.evaluateMetric(request.serviceId(), request.metricName(), request.value());

        return saved;
    }

    public MonitoringEvent ingestAlert(AlertIngestionRequest request) {
        Map<String, Object> payload = new HashMap<>();
        if (request.metadata() != null) payload.putAll(request.metadata());
        payload.put("alertName", request.alertName());
        if (request.correlationKey() != null) payload.put("correlationKey", request.correlationKey());

        MonitoringEvent event = MonitoringEvent.builder()
                .serviceId(request.serviceId())
                .eventType(MonitoringEventType.ALERT)
                .severity(request.severity())
                .source(request.source() != null ? request.source() : "EXTERNAL_MONITORING")
                .message(request.message())
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        MonitoringEvent saved = monitoringEventRepository.save(event);

        if (request.severity() == Severity.HIGH || request.severity() == Severity.CRITICAL) {
            String corrKey = request.correlationKey() != null ? request.correlationKey() : (request.serviceId() + ":" + request.alertName());
            CreateIncidentRequest incReq = new CreateIncidentRequest(
                    request.alertName(),
                    request.message(),
                    request.severity(),
                    request.serviceId(),
                    corrKey,
                    "Ingested from external monitoring source: " + request.source(),
                    List.of("external-alert", request.severity().name().toLowerCase()),
                    List.of(request.message())
            );
            try {
                incidentService.createIncident(incReq, "MONITORING_INGESTION");
            } catch (Exception ex) {
                log.error("Failed to escalate external alert to incident: {}", ex.getMessage());
            }
        }

        return saved;
    }

    public MonitoringEvent ingestHealth(HealthCheckIngestionRequest request) {
        Map<String, Object> payload = new HashMap<>();
        if (request.details() != null) payload.putAll(request.details());
        if (request.responseTimeMs() != null) payload.put("responseTimeMs", request.responseTimeMs());

        MonitoringEvent event = MonitoringEvent.builder()
                .serviceId(request.serviceId())
                .eventType(MonitoringEventType.HEALTH_CHECK)
                .source("HEALTH_INGESTION")
                .message(request.message() != null ? request.message() : "Health status: " + request.status())
                .payload(payload)
                .timestamp(Instant.now())
                .build();

        MonitoringEvent saved = monitoringEventRepository.save(event);

        // Update service registry
        try {
            serviceRegistryService.updateServiceStatus(request.serviceId(), request.status(), request.responseTimeMs());
        } catch (Exception ignored) {}

        return saved;
    }

    public List<MonitoringEvent> getRecentEvents(String serviceId, int limit) {
        return monitoringEventRepository.findByServiceIdOrderByTimestampDesc(serviceId, PageRequest.of(0, limit));
    }
}
