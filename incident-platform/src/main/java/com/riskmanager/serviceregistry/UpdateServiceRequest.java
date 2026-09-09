package com.riskmanager.serviceregistry;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateServiceRequest(
        @NotBlank(message = "Service name is required")
        String name,

        String description,

        @NotBlank(message = "Owner team is required")
        String ownerTeam,

        String environment,

        String healthEndpoint,

        List<String> dependencies,

        String repositoryUrl,

        String version
) {}
