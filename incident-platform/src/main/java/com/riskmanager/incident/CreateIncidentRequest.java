package com.riskmanager.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateIncidentRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Severity is required")
        Severity severity,

        @NotBlank(message = "Affected service ID is required")
        String affectedServiceId,

        String correlationKey,

        String impact,

        List<String> tags,

        List<String> logs
) {}
