package com.riskmanager.incident;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateIncidentRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Severity is required")
        Severity severity,

        String impact,

        String rootCause,

        String resolution,

        List<String> tags
) {}
