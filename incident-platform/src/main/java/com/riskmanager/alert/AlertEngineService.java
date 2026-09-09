package com.riskmanager.alert;

import com.riskmanager.incident.CreateIncidentRequest;
import com.riskmanager.incident.IncidentResponseDto;
import com.riskmanager.incident.IncidentService;
import com.riskmanager.incident.Severity;
import com.riskmanager.notification.NotificationDispatcher;
import com.riskmanager.notification.NotificationPayload;
import com.riskmanager.notification.NotificationType;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AlertEngineService {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertRepository alertRepository;
    private final IncidentService incidentService;
    private final ServiceRegistryService serviceRegistryService;
    private final NotificationDispatcher notificationDispatcher;

    public void evaluateMetric(String serviceId, String metricName, double value) {
        List<AlertRule> rules = alertRuleRepository.findByServiceIdAndEnabledTrue(serviceId);

        for (AlertRule rule : rules) {
            if (rule.getMetricName().equalsIgnoreCase(metricName)) {
                boolean conditionMet = checkCondition(rule.getCondition(), value, rule.getThreshold());
                if (conditionMet) {
                    processTriggeredRule(rule, "Metric " + metricName + " reached " + value + " (threshold: " + rule.getThreshold() + ")");
                }
            }
        }
    }

    public void evaluateLogEvent(String serviceId, Severity severity, String message) {
        if (severity == Severity.HIGH || severity == Severity.CRITICAL) {
            String serviceName = getServiceName(serviceId);
            String title = "[" + severity + "] Anomalous Log Pattern on " + serviceName;
            String correlationKey = serviceId + ":log_error:" + message.hashCode();

            Alert alert = Alert.builder()
                    .serviceId(serviceId)
                    .serviceName(serviceName)
                    .title(title)
                    .message(message)
                    .severity(severity)
                    .status(AlertStatus.ACTIVE)
                    .correlationKey(correlationKey)
                    .timestamp(Instant.now())
                    .build();

            Alert saved = alertRepository.save(alert);
            notifyAndEscalate(saved);
        }
    }

    private void processTriggeredRule(AlertRule rule, String triggerDetails) {
        Instant now = Instant.now();

        // Alert Storm Prevention: Check cooldown period
        if (rule.getLastTriggeredAt() != null) {
            long minutesSinceLast = Duration.between(rule.getLastTriggeredAt(), now).toMinutes();
            if (minutesSinceLast < rule.getCooldownMinutes()) {
                log.info("Alert rule '{}' triggered but suppressed under cooldown ({} min remaining)",
                        rule.getName(), (rule.getCooldownMinutes() - minutesSinceLast));
                return;
            }
        }

        rule.setLastTriggeredAt(now);
        alertRuleRepository.save(rule);

        String serviceName = getServiceName(rule.getServiceId());
        String correlationKey = rule.getServiceId() + ":" + rule.getMetricName();

        Alert alert = Alert.builder()
                .ruleId(rule.getId())
                .serviceId(rule.getServiceId())
                .serviceName(serviceName)
                .title("Alert: " + rule.getName())
                .message(triggerDetails)
                .severity(rule.getSeverity())
                .status(AlertStatus.ACTIVE)
                .correlationKey(correlationKey)
                .timestamp(now)
                .build();

        Alert saved = alertRepository.save(alert);
        log.warn("Triggered active alert: {} on service {}", saved.getTitle(), serviceName);

        notifyAndEscalate(saved);
    }

    private void notifyAndEscalate(Alert alert) {
        notificationDispatcher.dispatch(NotificationPayload.builder()
                .id(alert.getId())
                .type(NotificationType.ALERT_TRIGGERED)
                .title(alert.getTitle())
                .message(alert.getMessage())
                .severity(alert.getSeverity())
                .serviceId(alert.getServiceId())
                .serviceName(alert.getServiceName())
                .resourceId(alert.getId())
                .timestamp(alert.getTimestamp())
                .build());

        // Escalation: High and Critical alerts automatically create or correlate an incident
        if (alert.getSeverity() == Severity.HIGH || alert.getSeverity() == Severity.CRITICAL) {
            try {
                CreateIncidentRequest incidentReq = new CreateIncidentRequest(
                        alert.getTitle(),
                        alert.getMessage(),
                        alert.getSeverity(),
                        alert.getServiceId(),
                        alert.getCorrelationKey(),
                        "Automated alert escalation from rule " + alert.getRuleId(),
                        List.of("automated-alert", alert.getSeverity().name().toLowerCase()),
                        List.of(alert.getMessage())
                );

                IncidentResponseDto incident = incidentService.createIncident(incidentReq, "ALERT_ENGINE");
                log.info("Alert escalated to incident ID: {}", incident.getId());
            } catch (Exception ex) {
                log.error("Failed to automatically escalate alert to incident: {}", ex.getMessage(), ex);
            }
        }
    }

    private boolean checkCondition(AlertCondition condition, double actual, double threshold) {
        return switch (condition) {
            case GREATER_THAN -> actual > threshold;
            case LESS_THAN -> actual < threshold;
            case EQUALS -> Math.abs(actual - threshold) < 0.0001;
        };
    }

    private String getServiceName(String serviceId) {
        try {
            Service s = serviceRegistryService.findServiceEntityById(serviceId);
            return s.getName();
        } catch (Exception e) {
            return serviceId;
        }
    }
}
