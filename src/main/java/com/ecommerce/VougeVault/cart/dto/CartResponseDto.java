package com.ecommerce.VougeVault.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class CartResponseDto {

    private Long cartId;
    private List<CartItemResponseDto> items;
    private BigDecimal totalPrice;


}
