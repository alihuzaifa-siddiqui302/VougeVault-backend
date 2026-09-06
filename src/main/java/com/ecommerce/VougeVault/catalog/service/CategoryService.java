package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.catalog.Repository.CategoryRepository;
import com.ecommerce.VougeVault.catalog.dto.CategoryDto;
import com.ecommerce.VougeVault.catalog.dto.CategoryResponseDto;
import com.ecommerce.VougeVault.catalog.entity.Category;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Caching(evict = {
            @CacheEvict(value = "categories", key = "'browsing'"),
            @CacheEvict(value = "categories", key = "'brand:' + #brandId")
    })
    public Category createCategory(CategoryDto dto, Long brandId) {
        Category category = new Category();
        category.setName(dto.getName());

        Brand brand = new Brand();
        brand.setId(brandId);
        category.setBrand(brand);
        return categoryRepository.save(category);
    }

    @Cacheable(value = "categories", key = "'brand:' + #brandId")
    public List<Category> getMyCategories(Long brandId) {
        return categoryRepository.findByBrandId(brandId);
    }

    @Caching(evict = {
            @CacheEvict(value = "categories", key = "'browsing'"),
            @CacheEvict(value = "categories", key = "'brand:' + #brandId")
    })
    public Category updateCategory(Long categoryId, CategoryDto dto, Long brandId) {
        Category category = categoryRepository.findByIdAndBrandId(categoryId, brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(dto.getName());
        return categoryRepository.save(category);
    }

    @Caching(evict = {
            @CacheEvict(value = "categories", key = "'browsing'"),
            @CacheEvict(value = "categories", key = "'brand:' + #brandId")
    })
    public void deleteCategory(Long categoryId, Long brandId) {
        Category category = categoryRepository.findByIdAndBrandId(categoryId, brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        categoryRepository.delete(category);
    }

    @Cacheable(value = "categories", key = "'browsing'")
    public List<CategoryResponseDto> getAllCategoriesForBrowsing() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryResponseDto(category.getId(), category.getName()))
                .collect(Collectors.toList());
    }
}