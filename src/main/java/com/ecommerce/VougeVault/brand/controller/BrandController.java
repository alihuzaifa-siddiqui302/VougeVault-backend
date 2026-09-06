package com.ecommerce.VougeVault.brand.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.brand.dto.BrandRegistrationDto;
import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.brand.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.bind.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @PostMapping("/register")
    public ResponseEntity<?> brandRegister(@Valid @RequestBody BrandRegistrationDto dto) {
        brandService.registerBrand(dto);
        return ResponseEntity.ok(Map.of(
                "message", "Brand registration submitted. Awaiting Super Admin approval."
        ));
    }

    @PutMapping("/{brandId}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Brand> updateBrand(
            @PathVariable Long brandId,
            @Valid @RequestBody BrandRegistrationDto dto,  // Same DTO, same address fields
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(brandService.updateBrand(brandId, dto, currentUser.getUserId()));
    }

}
