package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.catalog.Repository.CategoryRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductRepository;
import com.ecommerce.VougeVault.catalog.dto.ProductDto;
import com.ecommerce.VougeVault.catalog.dto.ProductFilterRequest;
import com.ecommerce.VougeVault.catalog.dto.ProductResponseDto;
import com.ecommerce.VougeVault.catalog.entity.Category;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.inventory.Repository.InventoryRepository;
import com.ecommerce.VougeVault.inventory.entity.Inventory;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductService productService;

    private Category sampleCategory;
    private Brand sampleBrand;
    private Product sampleProduct;
    private ProductDto productDto;
    private ProductVariant sampleVariant;


    @BeforeEach
    void setUp() {
        sampleBrand = new Brand();
        sampleBrand.setId(1L);
        sampleBrand.setName("Vouge Originals");

        sampleCategory = new Category();
        sampleCategory.setId(10L);
        sampleCategory.setName("Shirts");
        sampleCategory.setBrand(sampleBrand);

        sampleVariant = new ProductVariant();
        sampleVariant.setId(100L);
        sampleVariant.setSize("L");
        sampleVariant.setColor("Blue");
        sampleVariant.setPrice(new BigDecimal("49.99"));
        sampleVariant.setSku("VOU-SHIRT-BLU-L");

        sampleProduct = new Product();
        sampleProduct.setId(50L);
        sampleProduct.setName("Slim Fit Denim Shirt");
        sampleProduct.setDescription("Classic button-down denim shirt");
        sampleProduct.setCategory(sampleCategory);
        sampleProduct.setBrand(sampleBrand);
        sampleProduct.setVariants(List.of(sampleVariant));

        productDto = new ProductDto();
        productDto.setName("Slim Fit Denim Shirt");
        productDto.setDescription("Classic button-down denim shirt");
        productDto.setCategoryId(10L);
    }

    @Nested
    @DisplayName("createProduct() Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category does not exist for brand")
        void createProduct_ShouldThrowException_WhenCategoryNotFound() {
            when(categoryRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.createProduct(productDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found");

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should create and save product when category exists")
        void createProduct_ShouldSaveProduct_WhenCategoryExists() {
            when(categoryRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleCategory));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
                Product p = invocation.getArgument(0);
                p.setId(50L);
                return p;
            });

            Product created = productService.createProduct(productDto, 1L);

            ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(productCaptor.capture());
            Product savedProduct = productCaptor.getValue();

            assertThat(savedProduct.getName()).isEqualTo("Slim Fit Denim Shirt");
            assertThat(savedProduct.getDescription()).isEqualTo("Classic button-down denim shirt");
            assertThat(savedProduct.getCategory()).isEqualTo(sampleCategory);
            assertThat(savedProduct.getBrand().getId()).isEqualTo(1L);
            assertThat(created.getId()).isEqualTo(50L);
        }
    }

    @Nested
    @DisplayName("getMyProducts() & getMyProduct() Tests")
    class RetrieveProductTests {

        @Test
        @DisplayName("Should return all products for a specific brand")
        void getMyProducts_ShouldReturnBrandProducts() {
            when(productRepository.findByBrandId(1L)).thenReturn(List.of(sampleProduct));

            List<Product> result = productService.getMyProducts(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Slim Fit Denim Shirt");
            verify(productRepository).findByBrandId(1L);
        }

        @Test
        @DisplayName("Should return specific product by id and brandId")
        void getMyProduct_ShouldReturnProduct_WhenFound() {
            when(productRepository.findByIdAndBrandId(50L, 1L)).thenReturn(Optional.of(sampleProduct));

            Product result = productService.getMyProduct(50L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(50L);
            assertThat(result.getName()).isEqualTo("Slim Fit Denim Shirt");
            verify(productRepository).findByIdAndBrandId(50L, 1L);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product not found for brand")
        void getMyProduct_ShouldThrowException_WhenNotFound() {
            when(productRepository.findByIdAndBrandId(50L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getMyProduct(50L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");
        }
    }

    @Nested
    @DisplayName("updateProduct() Tests")
    class UpdateProductTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when category does not exist")
        void updateProduct_ShouldThrowException_WhenCategoryNotFound() {
            when(categoryRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(50L, productDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category not found");

            verify(productRepository, never()).findByIdAndBrandId(anyLong(), anyLong());
            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product does not exist")
        void updateProduct_ShouldThrowException_WhenProductNotFound() {
            when(categoryRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleCategory));
            when(productRepository.findByIdAndBrandId(50L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.updateProduct(50L, productDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");

            verify(productRepository, never()).save(any(Product.class));
        }

        @Test
        @DisplayName("Should update product fields and save when valid")
        void updateProduct_ShouldUpdateFieldsAndSave_WhenValid() {
            Category updatedCategory = new Category();
            updatedCategory.setId(11L);
            updatedCategory.setName("Casual Shirts");

            ProductDto updateDto = new ProductDto();
            updateDto.setName("Relaxed Denim Shirt");
            updateDto.setDescription("Updated description");
            updateDto.setCategoryId(11L);

            when(categoryRepository.findByIdAndBrandId(11L, 1L)).thenReturn(Optional.of(updatedCategory));
            when(productRepository.findByIdAndBrandId(50L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Product updated = productService.updateProduct(50L, updateDto, 1L);

            assertThat(updated.getName()).isEqualTo("Relaxed Denim Shirt");
            assertThat(updated.getDescription()).isEqualTo("Updated description");
            assertThat(updated.getCategory()).isEqualTo(updatedCategory);
            verify(productRepository).save(sampleProduct);
        }
    }

    @Nested
    @DisplayName("deleteProduct() Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when deleting non-existent product")
        void deleteProduct_ShouldThrowException_WhenProductNotFound() {
            when(productRepository.findByIdAndBrandId(50L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(50L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");

            verify(productRepository, never()).delete(any(Product.class));
        }

        @Test
        @DisplayName("Should delete product when found")
        void deleteProduct_ShouldDelete_WhenFound() {
            when(productRepository.findByIdAndBrandId(50L, 1L)).thenReturn(Optional.of(sampleProduct));

            productService.deleteProduct(50L, 1L);

            verify(productRepository).delete(sampleProduct);
        }
    }

    @Nested
    @DisplayName("browseProducts() Filter Matrix Tests")
    class BrowseProductsTests {

        private ProductFilterRequest filter;

        @BeforeEach
        void initFilter() {
            filter = new ProductFilterRequest();
        }

        @Test
        @DisplayName("Filter: Category + Brand + PriceRange")
        void browseProducts_WithCategoryAndBrandAndPriceRange() {
            filter.setCategoryId(10L);
            filter.setBrandId(1L);
            filter.setMinPrice(new BigDecimal("10.00"));
            filter.setMaxPrice(new BigDecimal("100.00"));

            when(productRepository.findByCategoryIdAndBrandIdAndVariants_PriceBetween(
                    10L, 1L, new BigDecimal("10.00"), new BigDecimal("100.00")
            )).thenReturn(List.of(sampleProduct));

            List<ProductResponseDto> result = productService.browseProducts(filter);

            assertThat(result).hasSize(1);
            verify(productRepository).findByCategoryIdAndBrandIdAndVariants_PriceBetween(10L, 1L, new BigDecimal("10.00"), new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Filter: Category + Brand (No PriceRange)")
        void browseProducts_WithCategoryAndBrand() {
            filter.setCategoryId(10L);
            filter.setBrandId(1L);

            when(productRepository.findByCategoryIdAndBrandId(10L, 1L)).thenReturn(List.of(sampleProduct));

            List<ProductResponseDto> result = productService.browseProducts(filter);

            assertThat(result).hasSize(1);
            verify(productRepository).findByCategoryIdAndBrandId(10L, 1L);
        }

        @Test
        @DisplayName("Filter: Category + PriceRange (No Brand)")
        void browseProducts_WithCategoryAndPriceRange() {
            filter.setCategoryId(10L);
            filter.setMinPrice(new BigDecimal("10.00"));
            filter.setMaxPrice(new BigDecimal("100.00"));

            when(productRepository.findByCategoryIdAndVariants_PriceBetween(
                    10L, new BigDecimal("10.00"), new BigDecimal("100.00")
            )).thenReturn(List.of(sampleProduct));

            List<ProductResponseDto> result = productService.browseProducts(filter);

            assertThat(result).hasSize(1);
            verify(productRepository).findByCategoryIdAndVariants_PriceBetween(10L, new BigDecimal("10.00"), new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Filter: Brand + PriceRange (No Category)")
        void browseProducts_WithBrandAndPriceRange() {
            filter.setBrandId(1L);
            filter.setMinPrice(new BigDecimal("10.00"));
            filter.setMaxPrice(new BigDecimal("100.00"));

            when(productRepository.findByBrandIdAndVariants_PriceBetween(
                    1L, new BigDecimal("10.00"), new BigDecimal("100.00")
            )).thenReturn(List.of(sampleProduct));

            List<ProductResponseDto> result = productService.browseProducts(filter);

            assertThat(result).hasSize(1);
            verify(productRepository).findByBrandIdAndVariants_PriceBetween(1L, new BigDecimal("10.00"), new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("Filter: Only Brand")
        void browseProducts_WithOnlyBrand() {
            filter.setBrandId(1L);

            when(productRepository.findByBrandId(1L)).thenReturn(List.of(sampleProduct));

            List<ProductResponseDto> result = productService.browseProducts(filter);

            assertThat(result).hasSize(1);
            verify(productRepository).findByBrandId(1L);
        }

        @Test
        @DisplayName("Filter: Only Category")
        void browseProducts_WithOnlyCategory() {
            filter.setCategoryId(10L);

            when(productRepository.findByCategoryId(10L)).thenReturn(List.of(sampleProduct));

            List<ProductResponseDto> result = productService.browseProducts(filter);

            assertThat(result).hasSize(1);
            verify(productRepository).findByCategoryId(10L);
        }

        @Test
        @DisplayName("Filter: No Filters (Find All)")
        void browseProducts_WithNoFilters() {
            when(productRepository.findAll()).thenReturn(List.of(sampleProduct));

            List<ProductResponseDto> result = productService.browseProducts(filter);

            assertThat(result).hasSize(1);
            verify(productRepository).findAll();
        }
    }

    @Nested
    @DisplayName("toResponseDto() Mapping Tests")
    class ToResponseDtoTests {

        @Test
        @DisplayName("Should map product and variants with stock quantity from inventory")
        void toResponseDto_ShouldMapVariantStock_WhenInventoryExists() {
            Inventory inventory = new Inventory();
            inventory.setId(500L);
            inventory.setStockQuantity(25);

            when(inventoryRepository.findByProductVariantId(100L)).thenReturn(Optional.of(inventory));

            ProductResponseDto dto = productService.toResponseDto(sampleProduct);

            assertThat(dto.getId()).isEqualTo(50L);
            assertThat(dto.getName()).isEqualTo("Slim Fit Denim Shirt");
            assertThat(dto.getDescription()).isEqualTo("Classic button-down denim shirt");
            assertThat(dto.getCategoryName()).isEqualTo("Shirts");
            assertThat(dto.getBrandName()).isEqualTo("Vouge Originals");

            assertThat(dto.getVariants()).hasSize(1);
            assertThat(dto.getVariants().get(0).getId()).isEqualTo(100L);
            assertThat(dto.getVariants().get(0).getSize()).isEqualTo("L");
            assertThat(dto.getVariants().get(0).getColour()).isEqualTo("Blue");
            assertThat(dto.getVariants().get(0).getPrice()).isEqualByComparingTo("49.99");
            assertThat(dto.getVariants().get(0).getSku()).isEqualTo("VOU-SHIRT-BLU-L");
            assertThat(dto.getVariants().get(0).getStockAvailable()).isEqualTo(25);
        }

        @Test
        @DisplayName("Should map stockQuantity to 0 when inventory record is not present")
        void toResponseDto_ShouldDefaultStockToZero_WhenInventoryNotFound() {
            when(inventoryRepository.findByProductVariantId(100L)).thenReturn(Optional.empty());

            ProductResponseDto dto = productService.toResponseDto(sampleProduct);

            assertThat(dto.getVariants()).hasSize(1);
            assertThat(dto.getVariants().get(0).getStockAvailable()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should map product with empty variant list")
        void toResponseDto_ShouldHandleEmptyVariants() {
            sampleProduct.setVariants(Collections.emptyList());

            ProductResponseDto dto = productService.toResponseDto(sampleProduct);

            assertThat(dto.getVariants()).isEmpty();
            verify(inventoryRepository, never()).findByProductVariantId(anyLong());
        }
    }
}