package com.financeapp.controller;

import com.financeapp.dto.response.ApiResponse;
import com.financeapp.dto.response.DashboardSummaryResponse;
import com.financeapp.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Aggregated analytics — ANALYST and ADMIN only")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
        summary = "Full dashboard summary",
        description = """
            Returns:
            - `totalIncome`, `totalExpenses`, `netBalance`
            - `categoryTotals` — map of category → total amount
            - `recentActivity` — last 10 transactions
            - `monthlyTrends`  — income vs expenses for the trailing 12 months
            """
    )
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getSummary()));
    }
}
