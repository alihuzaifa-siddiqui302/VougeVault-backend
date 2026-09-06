package com.ecommerce.VougeVault.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductResponseDto {
    private Long id;
    private String name;
    private String description;
    private String categoryName;
    private String brandName;
    private List<ProductVariantResponseDto> variants;
}
