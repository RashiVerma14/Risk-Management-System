package com.riskmanager.deployment;

import com.riskmanager.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deployments")
@RequiredArgsConstructor
@Tag(name = "Deployment Tracking", description = "Endpoints for tracking microservice deployments and deployment correlation")
public class DeploymentController {

    private final DeploymentService deploymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Record a new service deployment")
    public ResponseEntity<ApiResponse<Deployment>> recordDeployment(@Valid @RequestBody DeploymentRequest request) {
        Deployment deployment = deploymentService.recordDeployment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Deployment recorded successfully", deployment));
    }

    @GetMapping("/service/{serviceId}")
    @Operation(summary = "Get recent deployments for a microservice")
    public ResponseEntity<ApiResponse<List<Deployment>>> getDeploymentsForService(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Deployments retrieved",
                deploymentService.getRecentDeployments(serviceId, limit)));
    }

    @GetMapping
    @Operation(summary = "Get all recent deployments across services")
    public ResponseEntity<ApiResponse<List<Deployment>>> getAllDeployments(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Deployments retrieved",
                deploymentService.getAllRecentDeployments(limit)));
    }
}
