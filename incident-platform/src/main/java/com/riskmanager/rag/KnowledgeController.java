package com.riskmanager.rag;

import com.riskmanager.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base & RAG", description = "Endpoints for runbooks, postmortems, documentation, and semantic similarity search")
public class KnowledgeController {

    private final RagService ragService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ingest a new runbook or knowledge document (Admin only)")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> createDocument(
            @Valid @RequestBody KnowledgeDocumentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String author = userDetails != null ? userDetails.getUsername() : "ADMIN";
        KnowledgeDocument created = ragService.createDocument(request, author);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Document ingested successfully", created));
    }

    @GetMapping
    @Operation(summary = "List all knowledge base documents")
    public ResponseEntity<ApiResponse<List<KnowledgeDocument>>> getAllDocuments() {
        return ResponseEntity.ok(ApiResponse.ok("Documents retrieved successfully", ragService.getAllDocuments()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get specific knowledge document by ID")
    public ResponseEntity<ApiResponse<KnowledgeDocument>> getDocumentById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Document retrieved successfully", ragService.getDocumentById(id)));
    }

    @PostMapping("/search")
    @Operation(summary = "Search knowledge base for relevant runbooks and troubleshooting guides")
    public ResponseEntity<ApiResponse<List<KnowledgeDocument>>> searchDocuments(
            @Valid @RequestBody KnowledgeSearchRequest request) {

        int limit = request.limit() != null ? request.limit() : 5;
        List<KnowledgeDocument> results = ragService.retrieveRelevantDocuments(request.query(), request.serviceId(), limit);
        return ResponseEntity.ok(ApiResponse.ok("Search completed", results));
    }
}
