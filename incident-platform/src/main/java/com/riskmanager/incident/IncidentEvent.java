package com.riskmanager.incident;

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
@Document(collection = "incident_events")
public class IncidentEvent {

    @Id
    private String id;

    @Indexed
    private String incidentId;

    private EventType type;

    private String message;

    private String actor;

    private String source;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
