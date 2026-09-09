package com.riskmanager.serviceregistry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "services")
public class Service {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private String ownerTeam;

    @Builder.Default
    private ServiceStatus status = ServiceStatus.UNKNOWN;

    @Builder.Default
    private String environment = "production";

    private String healthEndpoint;

    @Builder.Default
    private List<String> dependencies = new ArrayList<>();

    private String repositoryUrl;

    @Builder.Default
    private String version = "1.0.0";

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();

    private Instant lastHealthCheckAt;

    private Long responseTimeMs;
}
