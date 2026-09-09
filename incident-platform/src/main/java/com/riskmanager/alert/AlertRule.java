package com.riskmanager.alert;

import com.riskmanager.incident.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alert_rules")
public class AlertRule {

    @Id
    private String id;

    private String name;

    @Indexed
    private String serviceId;

    private String metricName;

    private AlertCondition condition;

    private Double threshold;

    @Builder.Default
    private int durationMinutes = 1;

    private Severity severity;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private int cooldownMinutes = 5;

    private Instant lastTriggeredAt;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
