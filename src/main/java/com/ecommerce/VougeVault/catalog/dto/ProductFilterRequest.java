package com.ecommerce.VougeVault.catalog.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductFilterRequest {
    private Long categoryId;      // optional — filter by category
    private Long brandId;         // optional — filter by brand
    private BigDecimal minPrice;  // optional — filter by price range
    private BigDecimal maxPrice;

}
