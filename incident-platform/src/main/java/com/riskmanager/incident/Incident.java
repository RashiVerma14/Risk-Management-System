package com.riskmanager.incident;

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
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "incidents")
@CompoundIndexes({
        @CompoundIndex(name = "status_severity_idx", def = "{'status': 1, 'severity': 1}"),
        @CompoundIndex(name = "service_status_idx", def = "{'affectedServiceId': 1, 'status': 1}")
})
public class Incident {

    @Id
    private String id;

    private String title;

    private String description;

    @Indexed
    private Severity severity;

    @Indexed
    private IncidentStatus status;

    @Indexed
    private String affectedServiceId;

    @Indexed
    private String assignedEngineerId;

    @Indexed
    private String correlationKey;

    @Builder.Default
    private int eventCount = 1;

    private String impact;

    private String rootCause;

    private String resolution;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Builder.Default
    private List<String> logs = new ArrayList<>();

    @Builder.Default
    private Instant detectedAt = Instant.now();

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    private Instant acknowledgedAt;

    private Instant mitigatedAt;

    private Instant resolvedAt;

    private Instant closedAt;
}
