package com.ecommerce.VougeVault.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TrendDataPointDto {
    private LocalDate date;
    private BigDecimal revenue;
    private Long orderCount;
}