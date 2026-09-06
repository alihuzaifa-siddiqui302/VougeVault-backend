package com.ecommerce.VougeVault.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateAddToCartDto {

    @NotNull(message="Quantity is required")
    @Min(value=1,message="quantity must be atleast 1")
    private Integer quantity;
}
