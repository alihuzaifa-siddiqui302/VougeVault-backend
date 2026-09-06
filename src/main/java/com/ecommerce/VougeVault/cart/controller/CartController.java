package com.ecommerce.VougeVault.cart.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.cart.dto.AddToCartDto;
import com.ecommerce.VougeVault.cart.dto.CartResponseDto;
import com.ecommerce.VougeVault.cart.dto.UpdateAddToCartDto;
import com.ecommerce.VougeVault.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponseDto> getCart(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(cartService.getCart(currentUser.getUserId()));
    }

    @PostMapping
    public ResponseEntity<CartResponseDto> addToCart(
            @Valid @RequestBody AddToCartDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(cartService.addToCart(currentUser.getUserId(), dto));
    }

    @PutMapping("items/{cartItemId}")
    public ResponseEntity<CartResponseDto> updateQuantity(@PathVariable Long cartItemId,
                                                          @Valid @RequestBody UpdateAddToCartDto dto,
                                                          @AuthenticationPrincipal CustomUserDetails currentUser){
        return ResponseEntity.ok(cartService.updateQuantity(currentUser.getUserId(),cartItemId,dto ));
    }
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<CartResponseDto> removeItem(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(cartService.removeItem(currentUser.getUserId(), cartItemId));
    }

    @DeleteMapping
    public ResponseEntity<?> clearCart(@AuthenticationPrincipal CustomUserDetails currentUser) {
        cartService.clearCart(currentUser.getUserId());
        return ResponseEntity.ok(Map.of("message", "Cart cleared"));
    }
}
