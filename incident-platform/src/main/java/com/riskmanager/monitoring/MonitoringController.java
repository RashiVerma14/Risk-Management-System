package com.riskmanager.monitoring;

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
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@Tag(name = "Monitoring Ingestion", description = "Endpoints for ingesting application logs, metrics, alerts, and health-checks")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @PostMapping("/logs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Ingest application log events")
    public ResponseEntity<ApiResponse<MonitoringEvent>> ingestLog(@Valid @RequestBody LogIngestionRequest request) {
        MonitoringEvent event = monitoringService.ingestLog(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Log ingested successfully", event));
    }

    @PostMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Ingest service telemetry metrics")
    public ResponseEntity<ApiResponse<MonitoringEvent>> ingestMetric(@Valid @RequestBody MetricIngestionRequest request) {
        MonitoringEvent event = monitoringService.ingestMetric(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Metric ingested successfully", event));
    }

    @PostMapping("/alerts")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Ingest external monitoring alerts")
    public ResponseEntity<ApiResponse<MonitoringEvent>> ingestAlert(@Valid @RequestBody AlertIngestionRequest request) {
        MonitoringEvent event = monitoringService.ingestAlert(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Alert ingested successfully", event));
    }

    @PostMapping("/health")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Ingest manual or external health-check result")
    public ResponseEntity<ApiResponse<MonitoringEvent>> ingestHealth(@Valid @RequestBody HealthCheckIngestionRequest request) {
        MonitoringEvent event = monitoringService.ingestHealth(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Health status ingested successfully", event));
    }

    @GetMapping("/events/{serviceId}")
    @Operation(summary = "Get recent monitoring events for a service")
    public ResponseEntity<ApiResponse<List<MonitoringEvent>>> getEvents(
            @PathVariable String serviceId,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved successfully",
                monitoringService.getRecentEvents(serviceId, limit)));
    }
}
