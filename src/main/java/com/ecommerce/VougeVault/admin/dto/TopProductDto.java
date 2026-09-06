package com.ecommerce.VougeVault.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TopProductDto {
    private Long productId;
    private String productName;
    private BigDecimal revenue;
    private Long quantity;
}