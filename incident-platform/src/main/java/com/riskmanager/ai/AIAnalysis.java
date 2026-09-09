package com.riskmanager.ai;

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
@Document(collection = "ai_analyses")
public class AIAnalysis {

    @Id
    private String id;

    @Indexed
    private String incidentId;

    private String serviceId;

    private String serviceName;

    private String probableRootCause;

    private double confidenceScore;

    @Builder.Default
    private List<String> supportingEvidence = new ArrayList<>();

    @Builder.Default
    private List<String> recommendedActions = new ArrayList<>();

    @Builder.Default
    private List<String> affectedComponents = new ArrayList<>();

    @Builder.Default
    private List<String> uncertainties = new ArrayList<>();

    private String deploymentCorrelation;

    private String model;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
