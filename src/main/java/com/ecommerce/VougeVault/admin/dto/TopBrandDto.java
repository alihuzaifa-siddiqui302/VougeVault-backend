package com.ecommerce.VougeVault.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class TopBrandDto {
    private Long brandId;
    private String brandName;
    private BigDecimal revenue;
    private Long orderCount;
}
