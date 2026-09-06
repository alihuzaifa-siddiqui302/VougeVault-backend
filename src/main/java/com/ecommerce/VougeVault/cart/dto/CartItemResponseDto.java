package com.ecommerce.VougeVault.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class CartItemResponseDto {

    private Long cartItemId;
    private Long productVariantId;
    private String productName;
    private String size;
    private String colour;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
