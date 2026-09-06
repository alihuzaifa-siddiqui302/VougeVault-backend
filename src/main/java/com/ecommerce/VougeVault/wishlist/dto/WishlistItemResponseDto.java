package com.ecommerce.VougeVault.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class WishlistItemResponseDto {

    private Long wishlistItemId;
    private Long productVariantId;
    private String productName;
    private String size;
    private String colour;
    private BigDecimal PRICE;
}
