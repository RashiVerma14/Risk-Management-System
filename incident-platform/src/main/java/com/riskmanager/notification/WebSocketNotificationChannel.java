package com.riskmanager.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketNotificationChannel implements NotificationChannel {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public String getChannelName() {
        return "WEBSOCKET";
    }

    @Override
    public void send(NotificationPayload payload) {
        try {
            // General broadcast
            messagingTemplate.convertAndSend("/topic/notifications", payload);

            // Targeted topic based on payload type
            switch (payload.getType()) {
                case INCIDENT_CREATED, INCIDENT_UPDATED ->
                        messagingTemplate.convertAndSend("/topic/incidents", payload);
                case ALERT_TRIGGERED ->
                        messagingTemplate.convertAndSend("/topic/alerts", payload);
                case SERVICE_STATUS_CHANGED ->
                        messagingTemplate.convertAndSend("/topic/services", payload);
                default -> {}
            }
            log.debug("Broadcasted WebSocket notification: {}", payload.getTitle());
        } catch (Exception ex) {
            log.error("Failed to broadcast WebSocket notification: {}", ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
