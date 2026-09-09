package com.riskmanager.health;

import com.riskmanager.serviceregistry.ServiceStatus;
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
@Document(collection = "health_checks")
public class HealthCheckRecord {

    @Id
    private String id;

    @Indexed
    private String serviceId;

    private String serviceName;

    private ServiceStatus status;

    private Long responseTimeMs;

    private Integer httpStatusCode;

    private String errorMessage;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
