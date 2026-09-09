package com.riskmanager.notification;

import com.riskmanager.incident.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayload {

    private String id;
    private NotificationType type;
    private String title;
    private String message;
    private Severity severity;
    private String serviceId;
    private String serviceName;
    private String resourceId;
    @Builder.Default
    private Instant timestamp = Instant.now();
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
