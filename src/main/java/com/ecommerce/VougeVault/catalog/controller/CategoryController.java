package com.ecommerce.VougeVault.catalog.controller;


import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.catalog.dto.CategoryDto;
import com.ecommerce.VougeVault.catalog.dto.CategoryResponseDto;
import com.ecommerce.VougeVault.catalog.entity.Category;
import com.ecommerce.VougeVault.catalog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Category> create(
            @Valid @RequestBody CategoryDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Category category = categoryService.createCategory(dto, currentUser.getBrandId());
        return ResponseEntity.ok(category);
    }

    @GetMapping("/my-categories")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<List<Category>> getMyCategories(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(categoryService.getMyCategories(currentUser.getBrandId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<Category> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        Category category = categoryService.updateCategory(id, dto, currentUser.getBrandId());
        return ResponseEntity.ok(category);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BRAND_ADMIN')")
    public ResponseEntity<?> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        categoryService.deleteCategory(id, currentUser.getBrandId());
        return ResponseEntity.ok(Map.of("message", "Category deleted"));
    }

    @GetMapping("/all")
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategoriesForBrowsing());
    }
}