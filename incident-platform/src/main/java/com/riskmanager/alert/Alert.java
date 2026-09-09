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
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "alerts")
public class Alert {

    @Id
    private String id;

    private String ruleId;

    @Indexed
    private String serviceId;

    private String serviceName;

    private String title;

    private String message;

    @Indexed
    private Severity severity;

    @Indexed
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    @Indexed
    private String correlationKey;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
