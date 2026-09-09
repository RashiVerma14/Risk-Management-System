package com.riskmanager.deployment;

import jakarta.validation.constraints.NotBlank;

public record DeploymentRequest(
        @NotBlank(message = "Service ID is required")
        String serviceId,

        @NotBlank(message = "Version is required")
        String version,

        String environment,

        String deployedBy,

        String commitHash,

        String status,

        String changelog
) {}
