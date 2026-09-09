package com.riskmanager.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "knowledge_documents")
public class KnowledgeDocument {

    @Id
    private String id;

    private String title;

    private String content;

    @Indexed
    @Builder.Default
    private String documentType = "RUNBOOK"; // RUNBOOK, POSTMORTEM, ARCHITECTURE, FAQ

    @Indexed
    private String serviceId;

    private String serviceName;

    @Builder.Default
    private List<String> tags = new ArrayList<>();

    private String uploadedBy;

    @Builder.Default
    private Instant createdAt = Instant.now();

    @Builder.Default
    private Instant updatedAt = Instant.now();
}
