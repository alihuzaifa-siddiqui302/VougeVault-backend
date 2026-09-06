package com.ecommerce.VougeVault.wishlist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToWishlistDto {

    @NotNull(message = "Product variant is required")
    private Long productVariantId;
}
