package com.ecommerce.VougeVault.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserMetricsDto {
    private Long totalCustomers;
    private Long totalBrandAdmins;
    private Long totalDeliveryPersons;
}