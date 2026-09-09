package com.riskmanager.dashboard;

import com.riskmanager.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Operations Dashboard", description = "Endpoints for MTTR analytics, service availability, and operational KPIs")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(summary = "Get high-level operations dashboard metrics (MTTR, incident frequency, service availability)")
    public ResponseEntity<ApiResponse<DashboardMetricsDto>> getDashboardMetrics(
            @RequestParam(defaultValue = "24h") String window) {
        return ResponseEntity.ok(ApiResponse.ok("Dashboard metrics retrieved",
                dashboardService.getDashboardMetrics(window)));
    }
}
