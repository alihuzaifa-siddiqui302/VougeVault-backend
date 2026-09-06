package com.ecommerce.VougeVault.catalog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockUpdateDto {
    @NotNull(message = "Quantity is required")
    private Integer quantity;
}
