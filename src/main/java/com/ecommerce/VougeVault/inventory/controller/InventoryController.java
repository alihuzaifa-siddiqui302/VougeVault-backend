package com.ecommerce.VougeVault.inventory.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.inventory.dto.StockUpdateDto;
import com.ecommerce.VougeVault.inventory.entity.Inventory;
import com.ecommerce.VougeVault.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/{variantId}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Map<String, Integer>> getStock(
            @PathVariable Long variantId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Integer stock = inventoryService.getStock(variantId, currentUser.getBrandId());
        return ResponseEntity.ok(Map.of("stock", stock));
    }

    @PatchMapping("/{variantId}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Inventory> adjustStock(
            @PathVariable Long variantId,
            @Valid @RequestBody StockUpdateDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Inventory inventory = inventoryService.adjustStock(variantId, dto.getQuantity(), currentUser.getBrandId());
        return ResponseEntity.ok(inventory);
    }
}