package com.riskmanager.incident;

import com.riskmanager.exception.BadRequestException;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import com.riskmanager.user.Role;
import com.riskmanager.user.User;
import com.riskmanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentEventRepository incidentEventRepository;

    @Mock
    private ServiceRegistryService serviceRegistryService;

    @Mock
    private UserRepository userRepository;

    @Spy
    private IncidentStateMachine stateMachine = new IncidentStateMachine();

    @InjectMocks
    private IncidentService incidentService;

    private Service sampleService;
    private User sampleEngineer;
    private Incident sampleIncident;

    @BeforeEach
    void setUp() {
        sampleService = Service.builder()
                .id("srv-payment")
                .name("payment-service")
                .ownerTeam("Billing")
                .build();

        sampleEngineer = User.builder()
                .id("eng-1")
                .name("Jordan Engineer")
                .email("jordan@riskmanager.io")
                .role(Role.ENGINEER)
                .active(true)
                .build();

        sampleIncident = Incident.builder()
                .id("inc-1")
                .title("504 Gateway Timeout Surge")
                .description("Payment service 504 errors")
                .severity(Severity.HIGH)
                .status(IncidentStatus.OPEN)
                .affectedServiceId("srv-payment")
                .correlationKey("srv-payment:504_timeout")
                .eventCount(1)
                .logs(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void testCreateIncidentSuccess() {
        CreateIncidentRequest req = new CreateIncidentRequest(
                "504 Gateway Timeout Surge",
                "Payment service 504 errors",
                Severity.HIGH,
                "srv-payment",
                "srv-payment:504_timeout",
                "Checkout degraded",
                List.of("payment"),
                List.of("error log 1")
        );

        when(serviceRegistryService.findServiceEntityById("srv-payment")).thenReturn(sampleService);
        when(incidentRepository.findTopByCorrelationKeyAndStatusNotInOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class))).thenReturn(sampleIncident);

        IncidentResponseDto result = incidentService.createIncident(req, "test-user");

        assertNotNull(result);
        assertEquals("inc-1", result.getId());
        assertEquals(Severity.HIGH, result.getSeverity());
        assertEquals(1, result.getEventCount());
        verify(incidentEventRepository, times(1)).save(any(IncidentEvent.class));
    }

    @Test
    void testIncidentCorrelationDeduplication() {
        CreateIncidentRequest req = new CreateIncidentRequest(
                "504 Gateway Timeout Surge",
                "Repeated error occurrence",
                Severity.HIGH,
                "srv-payment",
                "srv-payment:504_timeout",
                "Checkout degraded",
                List.of("payment"),
                List.of("another error log")
        );

        when(serviceRegistryService.findServiceEntityById("srv-payment")).thenReturn(sampleService);
        // An active incident with this correlationKey already exists
        when(incidentRepository.findTopByCorrelationKeyAndStatusNotInOrderByCreatedAtDesc(eq("srv-payment:504_timeout"), any()))
                .thenReturn(Optional.of(sampleIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        IncidentResponseDto result = incidentService.createIncident(req, "test-user");

        assertNotNull(result);
        // Instead of creating a new incident, the eventCount was incremented to 2!
        assertEquals(2, result.getEventCount());
        verify(incidentEventRepository).save(argThat(event -> event.getType() == EventType.CORRELATED_EVENT_ADDED));
    }

    @Test
    void testAssignIncidentToEngineer() {
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident));
        when(userRepository.findById("eng-1")).thenReturn(Optional.of(sampleEngineer));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRegistryService.findServiceEntityById("srv-payment")).thenReturn(sampleService);

        IncidentResponseDto result = incidentService.assignIncident("inc-1", "eng-1", "admin-user");

        assertNotNull(result);
        assertEquals("eng-1", result.getAssignedEngineerId());
        // Status advances from OPEN to ACKNOWLEDGED on assignment
        assertEquals(IncidentStatus.ACKNOWLEDGED, result.getStatus());
        assertNotNull(result.getAcknowledgedAt());
    }

    @Test
    void testAssignToViewerThrowsBadRequest() {
        User viewer = User.builder()
                .id("view-1")
                .role(Role.VIEWER)
                .build();

        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident));
        when(userRepository.findById("view-1")).thenReturn(Optional.of(viewer));

        assertThrows(BadRequestException.class, () ->
                incidentService.assignIncident("inc-1", "view-1", "admin-user"));
    }

    @Test
    void testUpdateStatusToResolvedSetsResolvedAt() {
        sampleIncident.setStatus(IncidentStatus.INVESTIGATING);
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));
        when(serviceRegistryService.findServiceEntityById("srv-payment")).thenReturn(sampleService);

        IncidentResponseDto result = incidentService.updateStatus("inc-1", IncidentStatus.RESOLVED, "Fixed db pool", "eng-1");

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
        assertNotNull(result.getResolvedAt());
        verify(incidentEventRepository, times(2)).save(any(IncidentEvent.class));
    }
}
