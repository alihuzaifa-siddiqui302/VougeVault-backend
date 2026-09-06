package com.ecommerce.VougeVault.delivery.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.delivery.dto.DeliveryStatsDto;
import com.ecommerce.VougeVault.delivery.service.DeliveryStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deliveries/stats")
@RequiredArgsConstructor
public class DeliveryStatsController {

    private final DeliveryStatsService statsService;

    /**
     * GET /api/deliveries/stats/me
     * Get current delivery person's stats
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('DELIVERY_PERSON')")
    public ResponseEntity<DeliveryStatsDto> getMyStats(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(statsService.getStats(currentUser.getUserId()));
    }

    /**
     * GET /api/deliveries/stats/{deliveryPersonId}
     * Admin view delivery person stats
     */
    @GetMapping("/{deliveryPersonId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<DeliveryStatsDto> getStats(
            @PathVariable Long deliveryPersonId
    ) {
        return ResponseEntity.ok(statsService.getStats(deliveryPersonId));
    }
}