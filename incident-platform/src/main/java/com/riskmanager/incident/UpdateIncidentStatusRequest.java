package com.riskmanager.incident;

import jakarta.validation.constraints.NotNull;

public record UpdateIncidentStatusRequest(
        @NotNull(message = "Status is required")
        IncidentStatus status,

        String note
) {}
