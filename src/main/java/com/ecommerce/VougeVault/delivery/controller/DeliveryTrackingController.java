package com.ecommerce.VougeVault.delivery.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.delivery.dto.DeliveryLocationDto;
import com.ecommerce.VougeVault.delivery.dto.DeliveryTrackingDto;
import com.ecommerce.VougeVault.delivery.dto.UpdateLocationDto;
import com.ecommerce.VougeVault.delivery.service.DeliveryTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries/tracking")
@RequiredArgsConstructor
public class DeliveryTrackingController {

    private final DeliveryTrackingService trackingService;

    /**
     * POST /api/deliveries/tracking/{deliveryId}/location
     * Driver emits GPS coordinates -> written directly to Redis memory.
     */
    @PostMapping("/{deliveryId}/location")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<DeliveryLocationDto> updateLocation(
            @PathVariable Long deliveryId,
            @Valid @RequestBody UpdateLocationDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(trackingService.updateLocation(
                deliveryId, currentUser.getUserId(), dto
        ));
    }

    /**
     * GET /api/deliveries/tracking/order/{orderId}
     * Customer views live driver location -> read directly from Redis memory.
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<DeliveryTrackingDto> trackDelivery(
            @PathVariable Long orderId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(trackingService.trackDelivery(
                orderId, currentUser.getUserId()
        ));
    }
}