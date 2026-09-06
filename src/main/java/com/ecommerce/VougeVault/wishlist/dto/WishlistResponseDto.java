package com.ecommerce.VougeVault.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class WishlistResponseDto {

    private Long id;
    private List<WishlistItemResponseDto> itemDtos;
}
