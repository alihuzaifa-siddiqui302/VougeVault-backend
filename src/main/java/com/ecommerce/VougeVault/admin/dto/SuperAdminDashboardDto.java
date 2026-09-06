package com.ecommerce.VougeVault.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class SuperAdminDashboardDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private PlatformMetricsDto platformMetrics;
    private List<TopBrandDto> topBrands;
    private List<TopProductDto> topProducts;
    private UserMetricsDto userMetrics;
    private DeliveryMetricsDto deliveryMetrics;
    private List<TrendDataPointDto> trendData;
}