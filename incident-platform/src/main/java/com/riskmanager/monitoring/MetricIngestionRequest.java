package com.riskmanager.monitoring;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record MetricIngestionRequest(
        @NotBlank(message = "Service ID is required")
        String serviceId,

        @NotBlank(message = "Metric name is required")
        String metricName,

        @NotNull(message = "Metric value is required")
        Double value,

        String unit,

        Map<String, String> tags,

        String environment
) {}
