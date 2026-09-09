package com.riskmanager.deployment;

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
@Document(collection = "deployments")
public class Deployment {

    @Id
    private String id;

    @Indexed
    private String serviceId;

    private String serviceName;

    private String version;

    @Builder.Default
    private String environment = "production";

    @Builder.Default
    private Instant deployedAt = Instant.now();

    private String deployedBy;

    private String commitHash;

    @Builder.Default
    private String status = "SUCCESS";

    private String changelog;
}
