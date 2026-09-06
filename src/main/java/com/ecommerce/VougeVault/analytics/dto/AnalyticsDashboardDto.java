package com.ecommerce.VougeVault.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class AnalyticsDashboardDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private RevenueMetricsDto revenueMetrics;
    private OrderMetricsDto orderMetrics;
    private List<TopProductDto> topProductsByRevenue;
    private List<TopProductDto> topProductsByQuantity;
    private List<TrendDataPointDto> trendData;
}