package com.ecommerce.VougeVault.returnmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ReturnItemResponseDto {
    private Long orderItemId;
    private String productName;
    private Integer quantityReturned;
    private BigDecimal refundPerUnit;
    private BigDecimal totalRefund;
}