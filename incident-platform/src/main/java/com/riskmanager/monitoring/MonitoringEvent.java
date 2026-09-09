package com.riskmanager.monitoring;

import com.riskmanager.incident.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "monitoring_events")
@CompoundIndexes({
        @CompoundIndex(name = "service_timestamp_idx", def = "{'serviceId': 1, 'timestamp': -1}"),
        @CompoundIndex(name = "type_timestamp_idx", def = "{'eventType': 1, 'timestamp': -1}")
})
public class MonitoringEvent {

    @Id
    private String id;

    @Indexed
    private String serviceId;

    @Indexed
    private MonitoringEventType eventType;

    private Severity severity;

    private String source;

    private String message;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private String environment = "production";

    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
}
