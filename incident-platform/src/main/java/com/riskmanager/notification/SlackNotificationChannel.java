package com.riskmanager.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class SlackNotificationChannel implements NotificationChannel {

    private final String webhookUrl;
    private final RestClient restClient;

    public SlackNotificationChannel(
            @Value("${notification.slack.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public String getChannelName() {
        return "SLACK";
    }

    @Override
    public void send(NotificationPayload payload) {
        if (!isEnabled()) {
            log.debug("Slack notification skipped (no webhook configured): {}", payload.getTitle());
            return;
        }

        try {
            String emoji = switch (payload.getSeverity() != null ? payload.getSeverity() : com.riskmanager.incident.Severity.LOW) {
                case CRITICAL -> ":rotating_light:";
                case HIGH -> ":warning:";
                case MEDIUM -> ":information_source:";
                case LOW -> ":white_check_mark:";
            };

            String slackMessage = String.format("%s *[%s] %s*\n%s\n*Service:* %s | *Resource:* %s",
                    emoji, payload.getSeverity(), payload.getTitle(), payload.getMessage(),
                    payload.getServiceName() != null ? payload.getServiceName() : "N/A",
                    payload.getResourceId() != null ? payload.getResourceId() : "N/A");

            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", slackMessage))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully posted notification to Slack: {}", payload.getTitle());
        } catch (Exception ex) {
            log.warn("Failed to post notification to Slack webhook: {}", ex.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank() && webhookUrl.startsWith("http");
    }
}
