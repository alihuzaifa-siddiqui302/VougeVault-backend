package com.ecommerce.VougeVault.inventory.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockUpdateDto {
    @NotNull(message = "Quantity is required")
    private Integer quantity;
}
