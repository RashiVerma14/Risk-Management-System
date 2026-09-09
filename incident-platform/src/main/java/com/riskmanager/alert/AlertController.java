package com.riskmanager.alert;

import com.riskmanager.common.ApiResponse;
import com.riskmanager.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerting Engine", description = "Endpoints for configuring alert rules and managing triggered operational alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertRuleRepository alertRuleRepository;

    @GetMapping
    @Operation(summary = "List recent alerts")
    public ResponseEntity<ApiResponse<List<Alert>>> getAlerts(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        List<Alert> alerts = (status != null)
                ? alertRepository.findByStatusOrderByTimestampDesc(status)
                : alertRepository.findAll(PageRequest.of(page, size)).getContent();

        return ResponseEntity.ok(ApiResponse.ok("Alerts retrieved successfully", alerts));
    }

    @GetMapping("/rules")
    @Operation(summary = "List all configured alert rules")
    public ResponseEntity<ApiResponse<List<AlertRule>>> getRules(
            @RequestParam(required = false) String serviceId) {

        List<AlertRule> rules = (serviceId != null && !serviceId.isBlank())
                ? alertRuleRepository.findByServiceId(serviceId)
                : alertRuleRepository.findAll();

        return ResponseEntity.ok(ApiResponse.ok("Alert rules retrieved successfully", rules));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create an alert rule with thresholds and cooldown (Admin only)")
    public ResponseEntity<ApiResponse<AlertRule>> createRule(@Valid @RequestBody AlertRule rule) {
        rule.setCreatedAt(Instant.now());
        rule.setUpdatedAt(Instant.now());
        AlertRule saved = alertRuleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Alert rule created successfully", saved));
    }

    @PatchMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Acknowledge an active alert")
    public ResponseEntity<ApiResponse<Alert>> acknowledgeAlert(@PathVariable String id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        Alert saved = alertRepository.save(alert);
        return ResponseEntity.ok(ApiResponse.ok("Alert acknowledged", saved));
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER')")
    @Operation(summary = "Mark an alert as resolved")
    public ResponseEntity<ApiResponse<Alert>> resolveAlert(@PathVariable String id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found with id: " + id));
        alert.setStatus(AlertStatus.RESOLVED);
        Alert saved = alertRepository.save(alert);
        return ResponseEntity.ok(ApiResponse.ok("Alert marked as resolved", saved));
    }
}
