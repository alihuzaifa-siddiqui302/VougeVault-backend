package com.ecommerce.VougeVault.delivery.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.delivery.dto.VerifyOtpDto;
import com.ecommerce.VougeVault.delivery.service.DeliveryOtpService;
import com.ecommerce.VougeVault.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries/otp")
@RequiredArgsConstructor
public class DeliveryOtpController {

    private final DeliveryOtpService otpService;
    private final DeliveryService deliveryService;

    /**
     * POST /api/deliveries/otp/{deliveryId}/generate
     * Delivery person generates OTP when arriving
     */
    @PostMapping("/{deliveryId}/generate")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<String> generateOtp(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        otpService.generateAndSendOtp(deliveryId);
        return ResponseEntity.ok("OTP sent to customer via email");
    }

    /**
     * POST /api/deliveries/otp/{deliveryId}/verify
     * Customer verifies OTP
     */
    @PostMapping("/{deliveryId}/verify")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<String> verifyOtp(
            @PathVariable Long deliveryId,
            @Valid @RequestBody VerifyOtpDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        otpService.verifyOtp(deliveryId, dto.getOtpCode(), deliveryService);
        return ResponseEntity.ok("Delivery confirmed! Order marked as delivered.");
    }


}