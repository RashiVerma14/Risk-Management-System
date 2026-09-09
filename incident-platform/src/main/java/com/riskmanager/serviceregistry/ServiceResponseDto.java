package com.riskmanager.serviceregistry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponseDto {

    private String id;
    private String name;
    private String description;
    private String ownerTeam;
    private ServiceStatus status;
    private String environment;
    private String healthEndpoint;
    private List<String> dependencies;
    private String repositoryUrl;
    private String version;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastHealthCheckAt;
    private Long responseTimeMs;

    public static ServiceResponseDto from(Service service) {
        return ServiceResponseDto.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .ownerTeam(service.getOwnerTeam())
                .status(service.getStatus())
                .environment(service.getEnvironment())
                .healthEndpoint(service.getHealthEndpoint())
                .dependencies(service.getDependencies())
                .repositoryUrl(service.getRepositoryUrl())
                .version(service.getVersion())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .lastHealthCheckAt(service.getLastHealthCheckAt())
                .responseTimeMs(service.getResponseTimeMs())
                .build();
    }
}
