package com.riskmanager.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LlmClient {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int timeoutSeconds;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public LlmClient(
            @Value("${ai.base-url:https://api.groq.com/openai/v1}") String baseUrl,
            @Value("${ai.api-key:demo-key}") String apiKey,
            @Value("${ai.model:llama-3.3-70b-versatile}") String model,
            @Value("${ai.timeout-seconds:15}") int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.objectMapper = new ObjectMapper();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public AIAnalysis analyze(AiInvestigationContext context) {
        String serviceName = context.getAffectedService() != null ? context.getAffectedService().getName() : "Unknown";

        // If no real API key configured or test mode, perform deterministic heuristic RCA
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("demo-key") || apiKey.equals("test-key")) {
            return buildDeterministicHeuristicAnalysis(context);
        }

        try {
            String systemPrompt = """
                    You are a Staff SRE Root Cause Analysis Engine for RiskManager.
                    CRITICAL SAFETY RULES:
                    1. Only use the supplied evidence. Do NOT hallucinate logs, metrics, or incidents.
                    2. If evidence is ambiguous or insufficient, state uncertainties and set confidenceScore < 0.5.
                    3. Output MUST be valid JSON only, with exact keys:
                       "probableRootCause", "confidenceScore", "supportingEvidence", "recommendedActions", "affectedComponents", "uncertainties", "deploymentCorrelation"
                    """;

            String userPrompt = buildPromptFromEvidence(context);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object")
            );

            String responseString = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(responseString);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            JsonNode analysisNode = objectMapper.readTree(content);

            List<String> evidence = new ArrayList<>();
            if (analysisNode.has("supportingEvidence") && analysisNode.get("supportingEvidence").isArray()) {
                analysisNode.get("supportingEvidence").forEach(e -> evidence.add(e.asText()));
            }

            List<String> recommendations = new ArrayList<>();
            if (analysisNode.has("recommendedActions") && analysisNode.get("recommendedActions").isArray()) {
                analysisNode.get("recommendedActions").forEach(r -> recommendations.add(r.asText()));
            }

            List<String> affected = new ArrayList<>();
            if (analysisNode.has("affectedComponents") && analysisNode.get("affectedComponents").isArray()) {
                analysisNode.get("affectedComponents").forEach(a -> affected.add(a.asText()));
            }

            List<String> uncertainties = new ArrayList<>();
            if (analysisNode.has("uncertainties") && analysisNode.get("uncertainties").isArray()) {
                analysisNode.get("uncertainties").forEach(u -> uncertainties.add(u.asText()));
            }

            return AIAnalysis.builder()
                    .incidentId(context.getIncident().getId())
                    .serviceId(context.getIncident().getAffectedServiceId())
                    .serviceName(serviceName)
                    .probableRootCause(analysisNode.path("probableRootCause").asText("Undetermined root cause"))
                    .confidenceScore(analysisNode.path("confidenceScore").asDouble(0.5))
                    .supportingEvidence(evidence)
                    .recommendedActions(recommendations)
                    .affectedComponents(affected)
                    .uncertainties(uncertainties)
                    .deploymentCorrelation(analysisNode.path("deploymentCorrelation").asText(null))
                    .model(model)
                    .build();

        } catch (Exception ex) {
            log.warn("LLM API call failed or timed out ({}). Falling back to heuristic evidence analysis.", ex.getMessage());
            return buildDeterministicHeuristicAnalysis(context);
        }
    }

    private String buildPromptFromEvidence(AiInvestigationContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("INCIDENT:\n");
        sb.append("Title: ").append(ctx.getIncident().getTitle()).append("\n");
        sb.append("Description: ").append(ctx.getIncident().getDescription()).append("\n");
        sb.append("Severity: ").append(ctx.getIncident().getSeverity()).append("\n\n");

        if (ctx.getAffectedService() != null) {
            sb.append("SERVICE: ").append(ctx.getAffectedService().getName())
                    .append(" (Status: ").append(ctx.getAffectedService().getStatus()).append(")\n\n");
        }

        if (!ctx.getRecentDeployments().isEmpty()) {
            sb.append("RECENT DEPLOYMENTS:\n");
            ctx.getRecentDeployments().forEach(d ->
                    sb.append(String.format("- Version %s deployed at %s by %s: %s\n",
                            d.getVersion(), d.getDeployedAt(), d.getDeployedBy(), d.getChangelog())));
            sb.append("\n");
        }

        if (!ctx.getRecentLogs().isEmpty()) {
            sb.append("RECENT LOGS:\n");
            ctx.getRecentLogs().stream().limit(10).forEach(l ->
                    sb.append(String.format("- [%s] %s\n", l.getSeverity(), l.getMessage())));
            sb.append("\n");
        }

        if (!ctx.getRetrievedRunbooks().isEmpty()) {
            sb.append("RETRIEVED RUNBOOKS/DOCS:\n");
            ctx.getRetrievedRunbooks().forEach(rb ->
                    sb.append(String.format("- %s: %s\n", rb.getTitle(), rb.getContent())));
            sb.append("\n");
        }

        return sb.toString();
    }

    private AIAnalysis buildDeterministicHeuristicAnalysis(AiInvestigationContext ctx) {
        String serviceName = ctx.getAffectedService() != null ? ctx.getAffectedService().getName() : "Target Service";
        List<String> evidence = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        List<String> affected = new ArrayList<>();
        List<String> uncertainties = new ArrayList<>();

        affected.add(serviceName);

        String deploymentCorrelation = null;
        if (!ctx.getRecentDeployments().isEmpty()) {
            var dep = ctx.getRecentDeployments().get(0);
            deploymentCorrelation = String.format("Recent deployment of %s version %s occurred within the failure window.",
                    dep.getServiceName(), dep.getVersion());
            evidence.add("Deployment detected: " + dep.getServiceName() + " v" + dep.getVersion() + " at " + dep.getDeployedAt());
            recommendations.add("Consider canary rollback for version " + dep.getVersion());
        }

        if (!ctx.getRecentLogs().isEmpty()) {
            evidence.add("Analyzed " + ctx.getRecentLogs().size() + " recent error/warning telemetry logs.");
            recommendations.add("Inspect stack trace from log: " + ctx.getRecentLogs().get(0).getMessage());
        } else {
            uncertainties.add("Sparse log evidence available in monitoring timeline.");
        }

        if (!ctx.getRetrievedRunbooks().isEmpty()) {
            var rb = ctx.getRetrievedRunbooks().get(0);
            recommendations.add("Follow Runbook: '" + rb.getTitle() + "'");
        } else {
            recommendations.add("Check database connection pool and downstream dependency latency.");
        }

        double confidence = deploymentCorrelation != null ? 0.82 : 0.65;
        String rootCause = deploymentCorrelation != null
                ? "Probable regression introduced by recent deployment of " + serviceName + " version " + ctx.getRecentDeployments().get(0).getVersion()
                : "Service degradation on " + serviceName + " correlated with error rate surge in monitoring logs.";

        return AIAnalysis.builder()
                .incidentId(ctx.getIncident().getId())
                .serviceId(ctx.getIncident().getAffectedServiceId())
                .serviceName(serviceName)
                .probableRootCause(rootCause)
                .confidenceScore(confidence)
                .supportingEvidence(evidence)
                .recommendedActions(recommendations)
                .affectedComponents(affected)
                .uncertainties(uncertainties)
                .deploymentCorrelation(deploymentCorrelation)
                .model("heuristic-rules-engine (Groq fallback)")
                .build();
    }
}
