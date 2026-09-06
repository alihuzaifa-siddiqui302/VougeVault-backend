package com.ecommerce.VougeVault.brand.controller;

import com.ecommerce.VougeVault.brand.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/admin/brands")
@RequiredArgsConstructor
public class BrandApprovalController {
    private final BrandService brandService;

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        brandService.approveBrand(id);
        return ResponseEntity.ok(Map.of("message", "Brand approved"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> reject(@PathVariable Long id) {
        brandService.rejectBrand(id);
        return ResponseEntity.ok(Map.of("message", "Brand rejected"));
    }

}
