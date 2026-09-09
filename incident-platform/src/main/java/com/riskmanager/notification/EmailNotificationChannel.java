package com.riskmanager.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    private final boolean enabled;
    private final String fromEmail;

    public EmailNotificationChannel(
            @Value("${notification.email.enabled:false}") boolean enabled,
            @Value("${notification.email.from:alerts@riskmanager.io}") String fromEmail) {
        this.enabled = enabled;
        this.fromEmail = fromEmail;
    }

    @Override
    public String getChannelName() {
        return "EMAIL";
    }

    @Override
    public void send(NotificationPayload payload) {
        if (!isEnabled()) {
            log.debug("Email notification skipped (disabled): {}", payload.getTitle());
            return;
        }

        try {
            log.info("[EMAIL DISPATCH] From: {} | Subject: [{}] {} | Body: {}",
                    fromEmail, payload.getSeverity(), payload.getTitle(), payload.getMessage());
        } catch (Exception ex) {
            log.warn("Failed to dispatch email alert: {}", ex.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
