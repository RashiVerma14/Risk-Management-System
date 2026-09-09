package com.riskmanager.serviceregistry;

import jakarta.validation.constraints.NotNull;

public record UpdateServiceStatusRequest(
        @NotNull(message = "Service status is required")
        ServiceStatus status
) {}
