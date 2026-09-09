package com.riskmanager.rag;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeSearchRequest(
        @NotBlank(message = "Search query is required")
        String query,

        String serviceId,

        Integer limit
) {}
