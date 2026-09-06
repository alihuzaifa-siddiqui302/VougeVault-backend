package com.ecommerce.VougeVault.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class OrderMetricsDto {
    private Long totalOrders;
    private Long activeOrders;
    private Map<String, Long> ordersByStatus; // {PAID: 10, SHIPPED: 5, ...}
}
