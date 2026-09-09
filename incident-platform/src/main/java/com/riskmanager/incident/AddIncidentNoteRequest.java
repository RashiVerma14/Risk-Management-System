package com.riskmanager.incident;

import jakarta.validation.constraints.NotBlank;

public record AddIncidentNoteRequest(
        @NotBlank(message = "Note content is required")
        String note
) {}
