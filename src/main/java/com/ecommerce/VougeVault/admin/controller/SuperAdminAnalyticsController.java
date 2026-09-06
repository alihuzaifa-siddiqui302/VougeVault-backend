package com.ecommerce.VougeVault.admin.controller;

import com.ecommerce.VougeVault.admin.dto.SuperAdminDashboardDto;
import com.ecommerce.VougeVault.admin.service.SuperAdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminAnalyticsController {

    private final SuperAdminAnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<SuperAdminDashboardDto> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        // Default to last 30 days if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        SuperAdminDashboardDto dashboard = analyticsService.getDashboard(startDate, endDate);
        return ResponseEntity.ok(dashboard);
    }
}