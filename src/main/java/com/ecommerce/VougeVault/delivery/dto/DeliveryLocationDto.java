package com.ecommerce.VougeVault.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DeliveryLocationDto {
    private Long locationId;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;
}