package com.riskmanager.ai;

import com.riskmanager.event.EventProducer;
import com.riskmanager.event.KafkaTopicConfig;
import com.riskmanager.exception.ResourceNotFoundException;
import com.riskmanager.incident.*;
import com.riskmanager.notification.NotificationDispatcher;
import com.riskmanager.notification.NotificationPayload;
import com.riskmanager.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInvestigationService {

    private final IncidentService incidentService;
    private final IncidentRepository incidentRepository;
    private final AiEvidenceCollector aiEvidenceCollector;
    private final LlmClient llmClient;
    private final AIAnalysisRepository aiAnalysisRepository;
    private final EventProducer eventProducer;
    private final NotificationDispatcher notificationDispatcher;

    public AIAnalysis investigate(String incidentId, String actor) {
        Incident incident = incidentService.findIncidentEntityById(incidentId);

        // Advance to INVESTIGATING status if currently OPEN or ACKNOWLEDGED
        if (incident.getStatus() == IncidentStatus.OPEN || incident.getStatus() == IncidentStatus.ACKNOWLEDGED) {
            incidentService.updateStatus(incidentId, IncidentStatus.INVESTIGATING, "Initiated AI root-cause investigation", actor);
            incident = incidentService.findIncidentEntityById(incidentId);
        }

        incidentService.createTimelineEvent(
                incidentId,
                EventType.AI_INVESTIGATION_TRIGGERED,
                "AI Root Cause Analysis triggered by " + actor,
                actor,
                "AI_INVESTIGATION_ENGINE",
                Map.of("model", "llama-3.3-70b-versatile")
        );

        // 1. Gather all evidence (logs, alerts, deployments, dependency topology, runbooks)
        AiInvestigationContext context = aiEvidenceCollector.collectEvidence(incident);

        // 2. Perform evidence-based RCA with LLM (or heuristic fallback)
        AIAnalysis analysis = llmClient.analyze(context);
        analysis.setCreatedAt(Instant.now());

        // 3. Save analysis to MongoDB
        AIAnalysis saved = aiAnalysisRepository.save(analysis);

        // 4. Update incident with probable root cause
        incident.setRootCause(saved.getProbableRootCause());
        if (saved.getRecommendedActions() != null && !saved.getRecommendedActions().isEmpty()) {
            incident.setResolution("Recommended: " + String.join("; ", saved.getRecommendedActions()));
        }
        incident.setUpdatedAt(Instant.now());
        incidentRepository.save(incident);

        // 5. Append timeline event
        incidentService.createTimelineEvent(
                incidentId,
                EventType.AI_INVESTIGATION_COMPLETED,
                String.format("AI RCA Completed (Confidence: %.0f%%): %s",
                        saved.getConfidenceScore() * 100, saved.getProbableRootCause()),
                "AI_SYSTEM",
                "AI_INVESTIGATION_ENGINE",
                Map.of(
                        "confidenceScore", saved.getConfidenceScore(),
                        "probableRootCause", saved.getProbableRootCause(),
                        "actionsCount", saved.getRecommendedActions().size()
                )
        );

        // 6. Asynchronous events via Kafka & WebSocket
        eventProducer.send(KafkaTopicConfig.TOPIC_AI_ANALYSIS, incidentId, saved);

        notificationDispatcher.dispatch(NotificationPayload.builder()
                .id(saved.getId())
                .type(NotificationType.AI_ANALYSIS_COMPLETED)
                .title("AI Root Cause Analysis Ready for Incident " + incidentId)
                .message(saved.getProbableRootCause())
                .severity(incident.getSeverity())
                .serviceId(incident.getAffectedServiceId())
                .serviceName(saved.getServiceName())
                .resourceId(incidentId)
                .timestamp(Instant.now())
                .metadata(Map.of("confidence", saved.getConfidenceScore()))
                .build());

        log.info("AI investigation completed for incident {} with confidence {}", incidentId, saved.getConfidenceScore());
        return saved;
    }

    public AIAnalysis getLatestAnalysis(String incidentId) {
        return aiAnalysisRepository.findTopByIncidentIdOrderByCreatedAtDesc(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("No AI analysis found for incident: " + incidentId));
    }
}
