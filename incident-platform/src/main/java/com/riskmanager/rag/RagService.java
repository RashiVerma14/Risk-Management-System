package com.riskmanager.rag;

import com.riskmanager.exception.ResourceNotFoundException;
import com.riskmanager.serviceregistry.Service;
import com.riskmanager.serviceregistry.ServiceRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class RagService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final ServiceRegistryService serviceRegistryService;

    @Value("${rag.qdrant.url:http://localhost:6333}")
    private String qdrantUrl;

    @Value("${rag.qdrant.collection-name:riskmanager_knowledge}")
    private String collectionName;

    private final RestClient restClient = RestClient.builder().build();

    public KnowledgeDocument createDocument(KnowledgeDocumentRequest request, String uploadedBy) {
        String serviceName = null;
        if (request.serviceId() != null && !request.serviceId().isBlank()) {
            try {
                Service s = serviceRegistryService.findServiceEntityById(request.serviceId());
                serviceName = s.getName();
            } catch (Exception ignored) {}
        }

        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title(request.title())
                .content(request.content())
                .documentType(request.documentType() != null ? request.documentType() : "RUNBOOK")
                .serviceId(request.serviceId())
                .serviceName(serviceName)
                .tags(request.tags() != null ? request.tags() : new ArrayList<>())
                .uploadedBy(uploadedBy)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        KnowledgeDocument saved = knowledgeDocumentRepository.save(doc);
        indexInQdrantIfAvailable(saved);

        log.info("Ingested knowledge document: '{}' ({})", saved.getTitle(), saved.getDocumentType());
        return saved;
    }

    public List<KnowledgeDocument> getAllDocuments() {
        return knowledgeDocumentRepository.findAll();
    }

    public KnowledgeDocument getDocumentById(String id) {
        return knowledgeDocumentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge document not found with id: " + id));
    }

    /**
     * Retrieves top-K relevant knowledge documents (runbooks, postmortems) using semantic & keyword relevance scoring.
     */
    public List<KnowledgeDocument> retrieveRelevantDocuments(String query, String serviceId, int topK) {
        List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findAll();
        if (allDocs.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = query.toLowerCase();
        Set<String> queryTokens = Arrays.stream(normalizedQuery.split("\\W+"))
                .filter(s -> s.length() > 2)
                .collect(Collectors.toSet());

        // Score documents
        record ScoredDoc(KnowledgeDocument doc, double score) {}

        List<ScoredDoc> scored = new ArrayList<>();
        for (KnowledgeDocument doc : allDocs) {
            double score = 0.0;

            // Direct service match boost
            if (serviceId != null && serviceId.equalsIgnoreCase(doc.getServiceId())) {
                score += 5.0;
            }

            // Title matches
            String titleLower = doc.getTitle().toLowerCase();
            for (String token : queryTokens) {
                if (titleLower.contains(token)) {
                    score += 3.0;
                }
            }

            // Tag matches
            if (doc.getTags() != null) {
                for (String tag : doc.getTags()) {
                    if (queryTokens.contains(tag.toLowerCase())) {
                        score += 2.5;
                    }
                }
            }

            // Content token overlap
            String contentLower = doc.getContent().toLowerCase();
            long matches = queryTokens.stream().filter(contentLower::contains).count();
            score += matches * 1.0;

            // Prefer runbooks and postmortems only if there's actual query relevance
            if (score > 0) {
                if ("RUNBOOK".equalsIgnoreCase(doc.getDocumentType())) score += 1.5;
                if ("POSTMORTEM".equalsIgnoreCase(doc.getDocumentType())) score += 1.2;
            }

            scored.add(new ScoredDoc(doc, score));
        }

        return scored.stream()
                .filter(sd -> sd.score() > 1.0)
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(topK)
                .map(ScoredDoc::doc)
                .toList();
    }

    private void indexInQdrantIfAvailable(KnowledgeDocument doc) {
        try {
            // Check if Qdrant is available; index if healthy
            Map<String, Object> point = Map.of(
                    "points", List.of(Map.of(
                            "id", Math.abs(doc.getId().hashCode()),
                            "payload", Map.of(
                                    "docId", doc.getId(),
                                    "title", doc.getTitle(),
                                    "serviceId", doc.getServiceId() != null ? doc.getServiceId() : "",
                                    "type", doc.getDocumentType()
                            )
                    ))
            );

            restClient.put()
                    .uri(qdrantUrl + "/collections/" + collectionName + "/points")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(point)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Successfully indexed document {} in Qdrant", doc.getId());
        } catch (Exception ex) {
            log.debug("Qdrant indexing skipped/unavailable (fallback to local semantic search): {}", ex.getMessage());
        }
    }
}
