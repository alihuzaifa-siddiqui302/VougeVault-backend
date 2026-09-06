package com.ecommerce.VougeVault.order.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.order.dto.CheckoutRequestDto;
import com.ecommerce.VougeVault.order.dto.OrderResponseDto;
import com.ecommerce.VougeVault.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDto> checkout(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                      @RequestBody CheckoutRequestDto dto) {
        return ResponseEntity.ok(orderService.checkout(currentUser.getUserId(),  dto));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(@AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(orderService.getMyOrders(currentUser.getUserId()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getMyOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(orderService.getMyOrder(orderId, currentUser.getUserId()));
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, currentUser.getUserId()));
    }
}