package com.ecommerce.VougeVault.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductDto {
    @NotBlank(message = "name of product is required")
    private String name;
    @Size(max = 2000, message = "Description too long")
    private String description;

    @NotNull(message = "Category is required")
    private Long categoryId;
}
