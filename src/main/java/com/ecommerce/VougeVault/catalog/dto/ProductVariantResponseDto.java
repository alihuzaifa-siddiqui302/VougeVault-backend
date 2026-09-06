package com.ecommerce.VougeVault.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class ProductVariantResponseDto {
    private Long id;
    private String size;
    private String colour;
    private BigDecimal price;
    private String sku;
    private Integer stockAvailable;
}
