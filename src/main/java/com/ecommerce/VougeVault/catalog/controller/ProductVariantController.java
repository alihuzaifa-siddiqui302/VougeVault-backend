package com.ecommerce.VougeVault.catalog.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.catalog.dto.ProductVariantDto;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.catalog.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{productId}/variants")
@RequiredArgsConstructor
public class ProductVariantController {

    private final ProductVariantService variantService;

    @PostMapping
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<ProductVariant> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ProductVariant variant = variantService.addVariant(productId, dto, currentUser.getBrandId());
        return ResponseEntity.ok(variant);
    }

    @GetMapping
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<List<ProductVariant>> getVariants(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(variantService.getVariantsForProduct(productId, currentUser.getBrandId()));
    }

    @PutMapping("/{variantId}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<ProductVariant> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ProductVariant variant = variantService.updateVariant(productId, variantId, dto, currentUser.getBrandId());
        return ResponseEntity.ok(variant);
    }

    @DeleteMapping("/{variantId}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<?> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        variantService.deleteVariant(productId, variantId, currentUser.getBrandId());
        return ResponseEntity.ok(Map.of("message", "Variant deleted"));
    }
}
