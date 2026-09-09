package com.riskmanager.rag;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record KnowledgeDocumentRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        String documentType,

        String serviceId,

        List<String> tags
) {}
