package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.catalog.Repository.CategoryRepository;
import com.ecommerce.VougeVault.catalog.dto.CategoryDto;
import com.ecommerce.VougeVault.catalog.dto.CategoryResponseDto;
import com.ecommerce.VougeVault.catalog.entity.Category;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryDto categoryDto;
    private Category sampleCategory;
    private Brand sampleBrand;

    @BeforeEach
    void setUp() {
        categoryDto = new CategoryDto();
        categoryDto.setName("Footwear");

        sampleBrand = new Brand();
        sampleBrand.setId(5L);

        sampleCategory = new Category();
        sampleCategory.setId(101L);
        sampleCategory.setName("Footwear");
        sampleCategory.setBrand(sampleBrand);
    }

    @Nested
    @DisplayName("createCategory() Tests")
    class CreateCategoryTests {

        @Test
        @DisplayName("Should create and save a new category associated with brandId")
        void createCategory_ShouldSaveAndReturnCategory() {
            Long brandId = 5L;
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
                Category cat = invocation.getArgument(0);
                cat.setId(101L);
                return cat;
            });

            Category created = categoryService.createCategory(categoryDto, brandId);

            ArgumentCaptor<Category> categoryCaptor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(categoryCaptor.capture());
            Category savedCategory = categoryCaptor.getValue();

            assertThat(savedCategory.getName()).isEqualTo("Footwear");
            assertThat(savedCategory.getBrand()).isNotNull();
            assertThat(savedCategory.getBrand().getId()).isEqualTo(brandId);
            assertThat(created.getId()).isEqualTo(101L);
        }
    }

    @Nested
    @DisplayName("getMyCategories() Tests")
    class GetMyCategoriesTests {

        @Test
        @DisplayName("Should return list of categories belonging to the specified brand")
        void getMyCategories_ShouldReturnBrandCategories() {
            Long brandId = 5L;
            Category category2 = new Category();
            category2.setId(102L);
            category2.setName("Accessories");
            category2.setBrand(sampleBrand);

            when(categoryRepository.findByBrandId(brandId)).thenReturn(List.of(sampleCategory, category2));

            List<Category> result = categoryService.getMyCategories(brandId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getName()).isEqualTo("Footwear");
            assertThat(result.get(1).getName()).isEqualTo("Accessories");
            verify(categoryRepository).findByBrandId(brandId);
        }

        @Test
        @DisplayName("Should return empty list when brand has no categories")
        void getMyCategories_ShouldReturnEmptyList_WhenNoCategoriesFound() {
            Long brandId = 99L;
            when(categoryRepository.findByBrandId(brandId)).thenReturn(Collections.emptyList());

            List<Category> result = categoryService.getMyCategories(brandId);

            assertThat(result).isEmpty();
            verify(categoryRepository).findByBrandId(brandId);
        }
    }

    @Nested
    @DisplayName("updateCategory() Tests")
    class UpdateCategoryTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category does not exist for the brand")
        void updateCategory_ShouldThrowException_WhenCategoryNotFound() {
            Long categoryId = 101L;
            Long brandId = 5L;
            when(categoryRepository.findByIdAndBrandId(categoryId, brandId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(categoryId, categoryDto, brandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found");

            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        @DisplayName("Should update category name and save when found")
        void updateCategory_ShouldUpdateAndSave_WhenFound() {
            Long categoryId = 101L;
            Long brandId = 5L;
            CategoryDto updateDto = new CategoryDto();
            updateDto.setName("Casual Footwear");

            when(categoryRepository.findByIdAndBrandId(categoryId, brandId)).thenReturn(Optional.of(sampleCategory));
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Category updated = categoryService.updateCategory(categoryId, updateDto, brandId);

            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("Casual Footwear");
            verify(categoryRepository).save(sampleCategory);
        }
    }

    @Nested
    @DisplayName("deleteCategory() Tests")
    class DeleteCategoryTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent category for brand")
        void deleteCategory_ShouldThrowException_WhenCategoryNotFound() {
            Long categoryId = 101L;
            Long brandId = 5L;
            when(categoryRepository.findByIdAndBrandId(categoryId, brandId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(categoryId, brandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found");

            verify(categoryRepository, never()).delete(any(Category.class));
        }

        @Test
        @DisplayName("Should delete category when found")
        void deleteCategory_ShouldDelete_WhenFound() {
            Long categoryId = 101L;
            Long brandId = 5L;
            when(categoryRepository.findByIdAndBrandId(categoryId, brandId)).thenReturn(Optional.of(sampleCategory));

            categoryService.deleteCategory(categoryId, brandId);

            verify(categoryRepository).delete(sampleCategory);
        }
    }

    @Nested
    @DisplayName("getAllCategoriesForBrowsing() Tests")
    class GetAllCategoriesForBrowsingTests {

        @Test
        @DisplayName("Should return all categories mapped to CategoryResponseDto list")
        void getAllCategoriesForBrowsing_ShouldReturnMappedDtos() {
            Category category2 = new Category();
            category2.setId(102L);
            category2.setName("Sportswear");

            when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory, category2));

            List<CategoryResponseDto> result = categoryService.getAllCategoriesForBrowsing();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getId()).isEqualTo(101L);
            assertThat(result.get(0).getName()).isEqualTo("Footwear");
            assertThat(result.get(1).getId()).isEqualTo(102L);
            assertThat(result.get(1).getName()).isEqualTo("Sportswear");
            verify(categoryRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no categories exist")
        void getAllCategoriesForBrowsing_ShouldReturnEmptyList_WhenEmpty() {
            when(categoryRepository.findAll()).thenReturn(Collections.emptyList());

            List<CategoryResponseDto> result = categoryService.getAllCategoriesForBrowsing();

            assertThat(result).isEmpty();
            verify(categoryRepository).findAll();
        }
    }
}