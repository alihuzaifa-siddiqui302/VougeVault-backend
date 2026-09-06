package com.ecommerce.VougeVault.payment.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.payment.dto.PaymentResponseDto;
import com.ecommerce.VougeVault.payment.dto.PaymentResponseDto;
import com.ecommerce.VougeVault.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PaymentResponseDto> createPayment(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(paymentService.createPayment(orderId, currentUser.getUserId()));
    }
}
