package com.ecommerce.VougeVault.catalog.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.catalog.dto.ProductImageDto;
import com.ecommerce.VougeVault.catalog.dto.UploadImageResponseDto;
import com.ecommerce.VougeVault.catalog.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/products/images")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    // Upload image (Brand Admin only)
    @PostMapping("/upload/{productId}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<UploadImageResponseDto> uploadImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") Boolean isPrimary,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productImageService.uploadImage(productId, file, isPrimary));
    }

    // Get all images for a product (public)
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductImageDto>> getProductImages(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(productImageService.getProductImages(productId));
    }

    // Get primary image for a product (public)
    @GetMapping("/product/{productId}/primary")
    public ResponseEntity<ProductImageDto> getPrimaryImage(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(productImageService.getPrimaryImage(productId));
    }

    // Download image (public)
    @GetMapping("/download/{fileName}")
    public ResponseEntity<byte[]> downloadImage(
            @PathVariable String fileName
    ) throws IOException {
        byte[] imageData = productImageService.downloadImage(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageData);
    }

    // Delete image (Brand Admin only)
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        productImageService.deleteImage(imageId, currentUser.getBrandId());
        return ResponseEntity.noContent().build();
    }

    // Set as primary image (Brand Admin only)
    @PutMapping("/{imageId}/set-primary")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Void> setPrimaryImage(
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        productImageService.setPrimaryImage(imageId, currentUser.getBrandId());
        return ResponseEntity.ok().build();
    }
}