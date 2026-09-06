package com.ecommerce.VougeVault.delivery.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.delivery.dto.DeliveryResponseDto;
import com.ecommerce.VougeVault.delivery.dto.UpdateDeliveryStatusDto;
import com.ecommerce.VougeVault.delivery.service.DeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // Delivery Person endpoints
    @GetMapping("/my-deliveries")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<List<DeliveryResponseDto>> getMyDeliveries(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(deliveryService.getMyDeliveries(currentUser.getUserId()));
    }

    @PatchMapping("/{deliveryId}/status")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<DeliveryResponseDto> updateDeliveryStatus(
            @PathVariable Long deliveryId,
            @Valid @RequestBody UpdateDeliveryStatusDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(deliveryService.updateDeliveryStatus(deliveryId, currentUser.getUserId(), dto));
    }

//    // Customer endpoint
//    @GetMapping("/track/{orderId}")
//    @PreAuthorize("hasRole('CUSTOMER')")
//    public ResponseEntity<DeliveryResponseDto> trackDelivery(
//            @PathVariable Long orderId,
//            @AuthenticationPrincipal CustomUserDetails currentUser
//    ) {
//        // Verify customer owns this order (implicit via getDeliveryByOrderId, which gets the order's delivery)
//        return ResponseEntity.ok(deliveryService.(orderId));
//    }
}