package com.ecommerce.VougeVault.catalog.controller;
import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.catalog.dto.ProductDto;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductDto dto,
                                       @AuthenticationPrincipal   CustomUserDetails currentUser){
   Product product=productService.createProduct(dto,currentUser.getBrandId());
   return ResponseEntity.ok(product);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Product>updateProduct(@Valid @PathVariable Long ProductId,@RequestBody ProductDto dto,
                                                @AuthenticationPrincipal CustomUserDetails currentUser){
        Product product=productService.updateProduct(ProductId,dto, currentUser.getBrandId());
        return ResponseEntity.ok(product);
    }

    @GetMapping("/my-products")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<List<Product>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(productService.getMyProducts(currentUser.getBrandId()));
    }

    @GetMapping("/my-products/{id}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Product> getMyProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(productService.getMyProduct(id, currentUser.getBrandId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        productService.deleteProduct(id, currentUser.getBrandId());
        return ResponseEntity.ok(Map.of("message", "Product deleted"));
    }
}
