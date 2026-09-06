package com.ecommerce.VougeVault.analytics.controller;

import com.ecommerce.VougeVault.analytics.dto.AnalyticsDashboardDto;
import com.ecommerce.VougeVault.analytics.service.AnalyticsService;
import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BRAND_ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<AnalyticsDashboardDto> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        // Default to last 30 days if dates not provided
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        Long brandId = currentUser.getBrandId();
        if (brandId == null) {
            return ResponseEntity.badRequest().build();
        }

        AnalyticsDashboardDto dashboard = analyticsService.getDashboard(brandId, startDate, endDate);
        return ResponseEntity.ok(dashboard);
    }
}