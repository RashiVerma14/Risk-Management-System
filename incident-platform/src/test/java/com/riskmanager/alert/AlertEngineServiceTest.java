package com.riskmanager.alert;

import com.riskmanager.incident.CreateIncidentRequest;
import com.riskmanager.incident.IncidentResponseDto;
import com.riskmanager.incident.IncidentService;
import com.riskmanager.incident.Severity;
import com.riskmanager.notification.NotificationDispatcher;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertEngineServiceTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private IncidentService incidentService;

    @Mock
    private ServiceRegistryService serviceRegistryService;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private AlertEngineService alertEngineService;

    private AlertRule sampleRule;
    private Service sampleService;

    @BeforeEach
    void setUp() {
        sampleService = Service.builder()
                .id("srv-payment")
                .name("payment-service")
                .build();

        sampleRule = AlertRule.builder()
                .id("rule-1")
                .name("Error Rate Critical")
                .serviceId("srv-payment")
                .metricName("error_rate")
                .condition(AlertCondition.GREATER_THAN)
                .threshold(10.0)
                .severity(Severity.HIGH)
                .cooldownMinutes(5)
                .enabled(true)
                .build();
    }

    @Test
    void testMetricTriggersAlertWhenThresholdExceeded() {
        when(alertRuleRepository.findByServiceIdAndEnabledTrue("srv-payment"))
                .thenReturn(List.of(sampleRule));
        when(serviceRegistryService.findServiceEntityById("srv-payment")).thenReturn(sampleService);
        when(alertRepository.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        IncidentResponseDto mockIncident = IncidentResponseDto.builder().id("inc-auto-1").build();
        when(incidentService.createIncident(any(CreateIncidentRequest.class), any())).thenReturn(mockIncident);

        // Metric value 15.5 > threshold 10.0
        alertEngineService.evaluateMetric("srv-payment", "error_rate", 15.5);

        verify(alertRepository, times(1)).save(any(Alert.class));
        verify(incidentService, times(1)).createIncident(any(CreateIncidentRequest.class), eq("ALERT_ENGINE"));
        verify(notificationDispatcher, times(1)).dispatch(any());
    }

    @Test
    void testAlertCooldownSuppressesStorms() {
        // Rule triggered 1 minute ago, cooldown is 5 minutes
        sampleRule.setLastTriggeredAt(Instant.now().minus(Duration.ofMinutes(1)));

        when(alertRuleRepository.findByServiceIdAndEnabledTrue("srv-payment"))
                .thenReturn(List.of(sampleRule));

        // Metric exceeds threshold again
        alertEngineService.evaluateMetric("srv-payment", "error_rate", 20.0);

        // Alert suppressed due to cooldown!
        verify(alertRepository, never()).save(any(Alert.class));
        verify(incidentService, never()).createIncident(any(), any());
    }
}
