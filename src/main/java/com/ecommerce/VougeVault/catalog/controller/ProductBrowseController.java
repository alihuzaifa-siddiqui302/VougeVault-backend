package com.ecommerce.VougeVault.catalog.controller;

import com.ecommerce.VougeVault.catalog.dto.ProductFilterRequest;
import com.ecommerce.VougeVault.catalog.dto.ProductResponseDto;
import com.ecommerce.VougeVault.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.BitSet;
import java.util.List;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductBrowseController {
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> browse(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,        // ← add this
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        ProductFilterRequest filter = new ProductFilterRequest();
        filter.setCategoryId(categoryId);
        filter.setBrandId(brandId);                                // ← and this
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);

        return ResponseEntity.ok(productService.browseProducts(filter));
    }
}
