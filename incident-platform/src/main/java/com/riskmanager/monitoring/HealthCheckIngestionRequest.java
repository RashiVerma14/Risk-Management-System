package com.riskmanager.monitoring;

import com.riskmanager.serviceregistry.ServiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record HealthCheckIngestionRequest(
        @NotBlank(message = "Service ID is required")
        String serviceId,

        @NotNull(message = "Status is required")
        ServiceStatus status,

        Long responseTimeMs,

        String message,

        Map<String, Object> details
) {}
