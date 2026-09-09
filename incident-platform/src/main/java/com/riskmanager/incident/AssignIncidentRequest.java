package com.riskmanager.incident;

import jakarta.validation.constraints.NotBlank;

public record AssignIncidentRequest(
        @NotBlank(message = "Engineer ID is required")
        String engineerId
) {}
