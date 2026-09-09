package com.riskmanager.serviceregistry;

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
@RequestMapping("/api/services")
@RequiredArgsConstructor
@Tag(name = "Service Registry", description = "Endpoints for registering and discovering microservices and tracking dependency graphs")
public class ServiceController {

    private final ServiceRegistryService serviceRegistryService;

    @GetMapping
    @Operation(summary = "List all registered microservices")
    public ResponseEntity<ApiResponse<List<ServiceResponseDto>>> getAllServices() {
        return ResponseEntity.ok(ApiResponse.ok("Services retrieved successfully", serviceRegistryService.getAllServices()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get microservice details by ID")
    public ResponseEntity<ApiResponse<ServiceResponseDto>> getServiceById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Service retrieved successfully", serviceRegistryService.getServiceById(id)));
    }

    @GetMapping("/{id}/dependencies")
    @Operation(summary = "Get upstream and downstream dependency graph for a microservice")
    public ResponseEntity<ApiResponse<ServiceDependencyDto>> getServiceDependencies(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Service dependencies retrieved successfully", serviceRegistryService.getServiceDependencies(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new microservice (Admin only)")
    public ResponseEntity<ApiResponse<ServiceResponseDto>> createService(@Valid @RequestBody CreateServiceRequest request) {
        ServiceResponseDto created = serviceRegistryService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Service registered successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update microservice configuration (Admin only)")
    public ResponseEntity<ApiResponse<ServiceResponseDto>> updateService(
            @PathVariable String id,
            @Valid @RequestBody UpdateServiceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Service updated successfully", serviceRegistryService.updateService(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Update microservice operational status (Admin or Engineer)")
    public ResponseEntity<ApiResponse<ServiceResponseDto>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateServiceStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Service status updated successfully",
                serviceRegistryService.updateServiceStatus(id, request.status(), null)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deregister/delete a microservice (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable String id) {
        serviceRegistryService.deleteService(id);
        return ResponseEntity.ok(ApiResponse.ok("Service deregistered successfully", null));
    }
}
