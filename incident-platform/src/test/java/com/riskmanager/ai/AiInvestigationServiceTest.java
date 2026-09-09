package com.riskmanager.ai;

import com.riskmanager.deployment.Deployment;
import com.riskmanager.event.EventProducer;
import com.riskmanager.incident.Incident;
import com.riskmanager.incident.IncidentRepository;
import com.riskmanager.incident.IncidentService;
import com.riskmanager.incident.IncidentStatus;
import com.riskmanager.incident.Severity;
import com.riskmanager.notification.NotificationDispatcher;
import com.riskmanager.rag.KnowledgeDocument;
import com.riskmanager.serviceregistry.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiInvestigationServiceTest {

    @Mock
    private IncidentService incidentService;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AiEvidenceCollector aiEvidenceCollector;

    @Mock
    private LlmClient llmClient;

    @Mock
    private AIAnalysisRepository aiAnalysisRepository;

    @Mock
    private EventProducer eventProducer;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private AiInvestigationService aiInvestigationService;

    private Incident sampleIncident;
    private Service sampleService;

    @BeforeEach
    void setUp() {
        sampleService = Service.builder()
                .id("srv-payment")
                .name("payment-service")
                .build();

        sampleIncident = Incident.builder()
                .id("inc-1")
                .title("504 Gateway Timeout Surge")
                .description("Customer checkout timeouts")
                .severity(Severity.HIGH)
                .status(IncidentStatus.OPEN)
                .affectedServiceId("srv-payment")
                .detectedAt(Instant.now())
                .build();
    }

    @Test
    void testInvestigateSuccessfulWorkflow() {
        when(incidentService.findIncidentEntityById("inc-1")).thenReturn(sampleIncident);

        AiInvestigationContext context = AiInvestigationContext.builder()
                .incident(sampleIncident)
                .affectedService(sampleService)
                .recentDeployments(List.of(Deployment.builder().version("2.4.1").build()))
                .retrievedRunbooks(List.of(KnowledgeDocument.builder().title("Payment Runbook").build()))
                .build();

        when(aiEvidenceCollector.collectEvidence(any(Incident.class))).thenReturn(context);

        AIAnalysis generatedAnalysis = AIAnalysis.builder()
                .id("ai-1")
                .incidentId("inc-1")
                .serviceName("payment-service")
                .probableRootCause("Recent deployment 2.4.1 misconfigured HTTP timeout")
                .confidenceScore(0.88)
                .supportingEvidence(List.of("Deployment v2.4.1 detected 10m before outage"))
                .recommendedActions(List.of("Rollback payment-service to v2.4.0"))
                .model("llama-3.3-70b-versatile")
                .build();

        when(llmClient.analyze(any(AiInvestigationContext.class))).thenReturn(generatedAnalysis);
        when(aiAnalysisRepository.save(any(AIAnalysis.class))).thenAnswer(inv -> inv.getArgument(0));

        AIAnalysis result = aiInvestigationService.investigate("inc-1", "engineer-1");

        assertNotNull(result);
        assertEquals("Recent deployment 2.4.1 misconfigured HTTP timeout", result.getProbableRootCause());
        assertEquals(0.88, result.getConfidenceScore());
        assertTrue(result.getSupportingEvidence().size() > 0);

        // Verify status transition to INVESTIGATING
        verify(incidentService).updateStatus(eq("inc-1"), eq(IncidentStatus.INVESTIGATING), anyString(), eq("engineer-1"));
        // Verify analysis persistence
        verify(aiAnalysisRepository).save(any(AIAnalysis.class));
        // Verify notifications
        verify(notificationDispatcher).dispatch(any());
    }
}
