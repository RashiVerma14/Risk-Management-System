package com.riskmanager.incident;

import com.riskmanager.common.ApiResponse;
import com.riskmanager.common.PageResponse;
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
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Tag(name = "Incident Management", description = "Endpoints for managing incidents, lifecycles, assignments, and audit timelines")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Create or correlate a new incident (Engineer/Admin)")
    public ResponseEntity<ApiResponse<IncidentResponseDto>> createIncident(
            @Valid @RequestBody CreateIncidentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String actor = userDetails != null ? userDetails.getUsername() : "ANONYMOUS";
        IncidentResponseDto response = incidentService.createIncident(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Incident created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Query and paginate incidents with filters")
    public ResponseEntity<ApiResponse<PageResponse<IncidentResponseDto>>> getIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String engineerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<IncidentResponseDto> response = incidentService.getIncidents(status, severity, serviceId, engineerId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Incidents retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed information for a specific incident")
    public ResponseEntity<ApiResponse<IncidentResponseDto>> getIncidentById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Incident retrieved successfully", incidentService.getIncidentById(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Update incident attributes (severity, description, tags, rootCause)")
    public ResponseEntity<ApiResponse<IncidentResponseDto>> updateIncident(
            @PathVariable String id,
            @Valid @RequestBody UpdateIncidentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String actor = userDetails != null ? userDetails.getUsername() : "ANONYMOUS";
        return ResponseEntity.ok(ApiResponse.ok("Incident updated successfully", incidentService.updateIncident(id, request, actor)));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Assign incident to an engineer")
    public ResponseEntity<ApiResponse<IncidentResponseDto>> assignIncident(
            @PathVariable String id,
            @Valid @RequestBody AssignIncidentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String actor = userDetails != null ? userDetails.getUsername() : "ANONYMOUS";
        return ResponseEntity.ok(ApiResponse.ok("Incident assigned successfully", incidentService.assignIncident(id, request.engineerId(), actor)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Update incident lifecycle status with state machine enforcement")
    public ResponseEntity<ApiResponse<IncidentResponseDto>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateIncidentStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String actor = userDetails != null ? userDetails.getUsername() : "ANONYMOUS";
        return ResponseEntity.ok(ApiResponse.ok("Incident status updated successfully",
                incidentService.updateStatus(id, request.status(), request.note(), actor)));
    }

    @PostMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Append an investigation note to the incident timeline")
    public ResponseEntity<ApiResponse<IncidentResponseDto>> addNote(
            @PathVariable String id,
            @Valid @RequestBody AddIncidentNoteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String actor = userDetails != null ? userDetails.getUsername() : "ANONYMOUS";
        return ResponseEntity.ok(ApiResponse.ok("Note added to incident", incidentService.addNote(id, request.note(), actor)));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "Retrieve complete audit event history for an incident")
    public ResponseEntity<ApiResponse<List<IncidentEvent>>> getIncidentTimeline(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Incident timeline retrieved successfully", incidentService.getIncidentTimeline(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an incident and all its timeline events (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteIncident(@PathVariable String id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.ok(ApiResponse.ok("Incident deleted successfully", null));
    }
}
