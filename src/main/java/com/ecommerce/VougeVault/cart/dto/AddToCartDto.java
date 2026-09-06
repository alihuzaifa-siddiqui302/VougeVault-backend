package com.ecommerce.VougeVault.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddToCartDto {

    @NotNull(message=" Product variant is required")
    private Long productVariantId;

    @NotNull
    @Min(value=1,message="quantity must be atleast 1")
    private Integer quantity;
}
