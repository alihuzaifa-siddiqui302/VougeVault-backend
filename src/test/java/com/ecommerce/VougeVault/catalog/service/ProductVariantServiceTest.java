package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.catalog.Repository.ProductRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductVariantRepository;
import com.ecommerce.VougeVault.catalog.dto.ProductVariantDto;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.inventory.Repository.InventoryRepository;
import com.ecommerce.VougeVault.inventory.entity.Inventory;
import com.ecommerce.VougeVault.shared.exception.DuplicateResourceException;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductVariantServiceTest {

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private ProductVariantService productVariantService;

    private Product sampleProduct;
    private ProductVariant sampleVariant;
    private ProductVariantDto variantDto;

    @BeforeEach
    void setUp() {
        sampleProduct = new Product();
        sampleProduct.setId(10L);
        sampleProduct.setName("Linen Shirt");

        sampleVariant = new ProductVariant();
        sampleVariant.setId(100L);
        sampleVariant.setProduct(sampleProduct);
        sampleVariant.setSize("M");
        sampleVariant.setColor("Blue");
        sampleVariant.setPrice(new BigDecimal("39.99"));
        sampleVariant.setSku("10-M-BLUE");

        variantDto = new ProductVariantDto();
        variantDto.setSize("M");
        variantDto.setColor("Blue");
        variantDto.setPrice(new BigDecimal("39.99"));
        variantDto.setInitialStock(50);
    }

    @Nested
    @DisplayName("addVariant() Tests")
    class AddVariantTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product is not found for brand")
        void addVariant_ShouldThrowException_WhenProductNotFound() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productVariantService.addVariant(10L, variantDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");

            verify(variantRepository, never()).save(any(ProductVariant.class));
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when size and color combo already exists")
        void addVariant_ShouldThrowException_WhenDuplicateComboExists() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductIdAndSizeAndColor(10L, "M", "Blue"))
                    .thenReturn(Optional.of(sampleVariant));

            assertThatThrownBy(() -> productVariantService.addVariant(10L, variantDto, 1L))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Variant with size M and color Blue already exists");

            verify(variantRepository, never()).save(any(ProductVariant.class));
            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should save variant with uppercase SKU and create matching inventory with initial stock")
        void addVariant_ShouldSaveVariantAndInventory_WhenValid() {
            variantDto.setSize("m");
            variantDto.setColor("blue");

            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductIdAndSizeAndColor(10L, "m", "blue")).thenReturn(Optional.empty());
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> {
                ProductVariant v = invocation.getArgument(0);
                v.setId(100L);
                return v;
            });

            ProductVariant created = productVariantService.addVariant(10L, variantDto, 1L);

            // Verify Variant persistence
            ArgumentCaptor<ProductVariant> variantCaptor = ArgumentCaptor.forClass(ProductVariant.class);
            verify(variantRepository).save(variantCaptor.capture());
            ProductVariant savedVariant = variantCaptor.getValue();

            assertThat(savedVariant.getProduct()).isEqualTo(sampleProduct);
            assertThat(savedVariant.getSize()).isEqualTo("m");
            assertThat(savedVariant.getColor()).isEqualTo("blue");
            assertThat(savedVariant.getPrice()).isEqualByComparingTo("39.99");
            assertThat(savedVariant.getSku()).isEqualTo("10-M-BLUE");
            assertThat(created.getId()).isEqualTo(100L);

            // Verify Inventory persistence
            ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(inventoryCaptor.capture());
            Inventory savedInventory = inventoryCaptor.getValue();

            assertThat(savedInventory.getVariant()).isEqualTo(created);
            assertThat(savedInventory.getStockQuantity()).isEqualTo(50);
        }

        @Test
        @DisplayName("Should default inventory stockQuantity to 0 when initialStock is null")
        void addVariant_ShouldDefaultStockToZero_WhenInitialStockIsNull() {
            variantDto.setInitialStock(null);

            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductIdAndSizeAndColor(10L, "M", "Blue")).thenReturn(Optional.empty());
            when(variantRepository.save(any(ProductVariant.class))).thenReturn(sampleVariant);

            productVariantService.addVariant(10L, variantDto, 1L);

            ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(inventoryCaptor.capture());
            assertThat(inventoryCaptor.getValue().getStockQuantity()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getVariantsForProduct() Tests")
    class GetVariantsForProductTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product does not exist for brand")
        void getVariantsForProduct_ShouldThrowException_WhenProductNotFound() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productVariantService.getVariantsForProduct(10L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");

            verify(variantRepository, never()).findByProductId(anyLong());
        }

        @Test
        @DisplayName("Should return list of variants for valid product and brand")
        void getVariantsForProduct_ShouldReturnVariants_WhenProductFound() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByProductId(10L)).thenReturn(List.of(sampleVariant));

            List<ProductVariant> variants = productVariantService.getVariantsForProduct(10L, 1L);

            assertThat(variants).hasSize(1);
            assertThat(variants.get(0).getSku()).isEqualTo("10-M-BLUE");
            verify(variantRepository).findByProductId(10L);
        }
    }

    @Nested
    @DisplayName("updateVariant() Tests")
    class UpdateVariantTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product does not exist for brand")
        void updateVariant_ShouldThrowException_WhenProductNotFound() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productVariantService.updateVariant(10L, 100L, variantDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");

            verify(variantRepository, never()).findByIdAndProductId(anyLong(), anyLong());
            verify(variantRepository, never()).save(any(ProductVariant.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when variant does not exist for product")
        void updateVariant_ShouldThrowException_WhenVariantNotFound() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByIdAndProductId(100L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productVariantService.updateVariant(10L, 100L, variantDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Variant not found");

            verify(variantRepository, never()).save(any(ProductVariant.class));
        }

        @Test
        @DisplayName("Should update only price and return saved variant")
        void updateVariant_ShouldUpdatePriceAndSave_WhenValid() {
            ProductVariantDto updateDto = new ProductVariantDto();
            updateDto.setPrice(new BigDecimal("49.99"));

            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByIdAndProductId(100L, 10L)).thenReturn(Optional.of(sampleVariant));
            when(variantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ProductVariant updated = productVariantService.updateVariant(10L, 100L, updateDto, 1L);

            assertThat(updated.getPrice()).isEqualByComparingTo("49.99");
            assertThat(updated.getSize()).isEqualTo("M");
            assertThat(updated.getColor()).isEqualTo("Blue");
            verify(variantRepository).save(sampleVariant);
        }
    }

    @Nested
    @DisplayName("deleteVariant() Tests")
    class DeleteVariantTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when product is not found")
        void deleteVariant_ShouldThrowException_WhenProductNotFound() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productVariantService.deleteVariant(10L, 100L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found");

            verify(variantRepository, never()).delete(any(ProductVariant.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when variant is not found")
        void deleteVariant_ShouldThrowException_WhenVariantNotFound() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByIdAndProductId(100L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productVariantService.deleteVariant(10L, 100L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Variant not found");

            verify(variantRepository, never()).delete(any(ProductVariant.class));
        }

        @Test
        @DisplayName("Should delete variant when found")
        void deleteVariant_ShouldDeleteVariant_WhenFound() {
            when(productRepository.findByIdAndBrandId(10L, 1L)).thenReturn(Optional.of(sampleProduct));
            when(variantRepository.findByIdAndProductId(100L, 10L)).thenReturn(Optional.of(sampleVariant));

            productVariantService.deleteVariant(10L, 100L, 1L);

            verify(variantRepository).delete(sampleVariant);
        }
    }
}