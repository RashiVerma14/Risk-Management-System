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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AIAnalysisRepository aiAnalysisRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void testMttrAndAvailabilityCalculation() {
        Instant now = Instant.now();

        // Incident 1: took 30 minutes to resolve, 5 minutes to acknowledge
        Incident inc1 = Incident.builder()
                .id("inc-1")
                .severity(Severity.HIGH)
                .status(IncidentStatus.RESOLVED)
                .affectedServiceId("srv-payment")
                .createdAt(now.minus(Duration.ofMinutes(60)))
                .acknowledgedAt(now.minus(Duration.ofMinutes(55)))
                .resolvedAt(now.minus(Duration.ofMinutes(30)))
                .build();

        // Incident 2: took 10 minutes to resolve, 2 minutes to acknowledge
        Incident inc2 = Incident.builder()
                .id("inc-2")
                .severity(Severity.CRITICAL)
                .status(IncidentStatus.RESOLVED)
                .affectedServiceId("srv-auth")
                .createdAt(now.minus(Duration.ofMinutes(20)))
                .acknowledgedAt(now.minus(Duration.ofMinutes(18)))
                .resolvedAt(now.minus(Duration.ofMinutes(10)))
                .build();

        when(incidentRepository.findByCreatedAtBetween(any(), any())).thenReturn(List.of(inc1, inc2));

        Service s1 = Service.builder().id("s1").status(ServiceStatus.HEALTHY).build();
        Service s2 = Service.builder().id("s2").status(ServiceStatus.DEGRADED).build();
        when(serviceRepository.findAll()).thenReturn(List.of(s1, s2));

        when(alertRepository.count()).thenReturn(10L);
        when(alertRepository.countByStatus(AlertStatus.ACTIVE)).thenReturn(2L);
        when(aiAnalysisRepository.count()).thenReturn(4L);

        DashboardMetricsDto metrics = dashboardService.getDashboardMetrics("24h");

        assertNotNull(metrics);
        assertEquals(2, metrics.getTotalIncidents());
        assertEquals(1, metrics.getCriticalIncidents());
        // MTTR = (30 + 10) / 2 = 20.0 minutes
        assertEquals(20.0, metrics.getAverageMttrMinutes());
        // MTTA = (5 + 2) / 2 = 3.5 minutes
        assertEquals(3.5, metrics.getAverageMttaMinutes());
        // Service availability = 1 healthy out of 2 = 50.0%
        assertEquals(50.0, metrics.getServiceAvailabilityPercentage());
        assertEquals(10L, metrics.getTotalAlerts());
        assertEquals(2L, metrics.getActiveAlerts());
    }
}
