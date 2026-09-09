package com.riskmanager.ai;

import com.riskmanager.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "AI Investigation", description = "Endpoints for triggering AI-driven root cause analysis and retrieving evidence-backed diagnostics")
public class AiInvestigationController {

    private final AiInvestigationService aiInvestigationService;

    @PostMapping("/{id}/investigate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Trigger AI Root Cause Analysis on an active incident (Engineer/Admin)")
    public ResponseEntity<ApiResponse<AIAnalysis>> triggerInvestigation(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {

        String actor = userDetails != null ? userDetails.getUsername() : "ENGINEER";
        AIAnalysis analysis = aiInvestigationService.investigate(id, actor);
        return ResponseEntity.ok(ApiResponse.ok("AI Root Cause Analysis completed", analysis));
    }

    @GetMapping("/{id}/analysis")
    @Operation(summary = "Retrieve latest AI Root Cause Analysis for an incident")
    public ResponseEntity<ApiResponse<AIAnalysis>> getAnalysis(@PathVariable String id) {
        AIAnalysis analysis = aiInvestigationService.getLatestAnalysis(id);
        return ResponseEntity.ok(ApiResponse.ok("AI Analysis retrieved", analysis));
    }
}
