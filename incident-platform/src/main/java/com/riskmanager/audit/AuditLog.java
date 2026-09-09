package com.riskmanager.audit;

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
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String actor;

    private String action;

    @Indexed
    private String resource;

    private String resourceId;

    private String ipAddress;

    @Builder.Default
    private Instant timestamp = Instant.now();

    @Builder.Default
    private Map<String, Object> details = new HashMap<>();
}
