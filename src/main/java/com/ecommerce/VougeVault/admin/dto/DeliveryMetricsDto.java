package com.ecommerce.VougeVault.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeliveryMetricsDto {
    private Double onTimeDeliveryRate;
    private Double averageDeliveryDays;
    private Long totalDeliveries;
    private Long onTimeDeliveries;
}