package com.riskmanager.monitoring;

import com.riskmanager.incident.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record LogIngestionRequest(
        @NotBlank(message = "Service ID or name is required")
        String serviceId,

        @NotNull(message = "Severity level is required")
        Severity level,

        @NotBlank(message = "Log message is required")
        String message,

        String logger,

        String traceId,

        String environment,

        Map<String, Object> context
) {}
