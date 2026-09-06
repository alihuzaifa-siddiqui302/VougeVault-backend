package com.ecommerce.VougeVault.returnmanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnItemRequestDto {

    @NotNull(message = "Order item ID is required")
    private Long orderItemId;

    @NotNull(message = "Quantity to return is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}