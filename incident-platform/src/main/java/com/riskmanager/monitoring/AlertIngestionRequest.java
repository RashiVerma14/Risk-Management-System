package com.riskmanager.monitoring;

import com.riskmanager.incident.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AlertIngestionRequest(
        @NotBlank(message = "Service ID is required")
        String serviceId,

        @NotBlank(message = "Alert name is required")
        String alertName,

        @NotNull(message = "Severity is required")
        Severity severity,

        @NotBlank(message = "Alert message is required")
        String message,

        String correlationKey,

        String source,

        Map<String, Object> metadata
) {}
