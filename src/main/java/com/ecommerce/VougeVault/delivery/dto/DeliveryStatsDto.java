package com.ecommerce.VougeVault.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class DeliveryStatsDto {
    private Integer totalDeliveries;
    private Integer successfulDeliveries;
    private Integer failedDeliveries;
    private Double successRate;  // 95.5%
    private BigDecimal averageRating;  // 4.5 stars
    private BigDecimal totalEarnings;
}