package com.ecommerce.VougeVault.wishlist.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.wishlist.dto.AddToWishlistDto;
import com.ecommerce.VougeVault.wishlist.dto.WishlistResponseDto;
import com.ecommerce.VougeVault.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<WishlistResponseDto> getWishlist(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(wishlistService.getWishlist(currentUser.getUserId()));
    }

    @PostMapping
    public ResponseEntity<WishlistResponseDto> addToWishlist(
            @Valid @RequestBody AddToWishlistDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(wishlistService.addToWishlist(currentUser.getUserId(), dto));
    }

    @DeleteMapping("/items/{wishlistItemId}")
    public ResponseEntity<WishlistResponseDto> removeItem(
            @PathVariable Long wishlistItemId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(wishlistService.removeFromWishlist(currentUser.getUserId(), wishlistItemId));
    }
}
