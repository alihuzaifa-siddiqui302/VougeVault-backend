package com.ecommerce.VougeVault.inventory.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.catalog.Repository.ProductVariantRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private ProductVariant sampleVariant;
    private Inventory sampleInventory;
    private final Long variantId = 100L;
    private final Long brandId = 1L;

    @BeforeEach
    void setUp() {
        Brand brand = new Brand();
        brand.setId(brandId);

        Product product = new Product();
        product.setId(10L);
        product.setBrand(brand);

        sampleVariant = new ProductVariant();
        sampleVariant.setId(variantId);
        sampleVariant.setProduct(product);

        sampleInventory = new Inventory();
        sampleInventory.setId(500L);
        sampleInventory.setVariant(sampleVariant);
        sampleInventory.setStockQuantity(20);
    }

    @Nested
    @DisplayName("getStock() Tests")
    class GetStockTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when variant does not exist")
        void getStock_ShouldThrowException_WhenVariantNotFound() {
            when(variantRepository.findById(variantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.getStock(variantId, brandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Variant not found");

            verify(inventoryRepository, never()).findByProductVariantId(anyLong());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when variant belongs to a different brand")
        void getStock_ShouldThrowException_WhenBrandMismatch() {
            Long unauthorizedBrandId = 999L;
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));

            assertThatThrownBy(() -> inventoryService.getStock(variantId, unauthorizedBrandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Variant not found");

            verify(inventoryRepository, never()).findByProductVariantId(anyLong());
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when inventory record does not exist")
        void getStock_ShouldThrowException_WhenInventoryNotFound() {
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.getStock(variantId, brandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Inventory record not found");
        }

        @Test
        @DisplayName("Should return current stock quantity when variant and inventory exist")
        void getStock_ShouldReturnStockQuantity_WhenValid() {
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.of(sampleInventory));

            Integer stock = inventoryService.getStock(variantId, brandId);

            assertThat(stock).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("adjustStock() Tests")
    class AdjustStockTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when inventory record does not exist")
        void adjustStock_ShouldThrowException_WhenInventoryNotFound() {
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.adjustStock(variantId, 5, brandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Inventory record not found");

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when reducing stock below 0")
        void adjustStock_ShouldThrowException_WhenResultingQuantityIsNegative() {
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.of(sampleInventory));

            assertThatThrownBy(() -> inventoryService.adjustStock(variantId, -25, brandId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Insufficient stock: cannot reduce below 0");

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should increment stock quantity and save inventory")
        void adjustStock_ShouldIncreaseStock_WhenQuantityChangeIsPositive() {
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.of(sampleInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

            Inventory result = inventoryService.adjustStock(variantId, 10, brandId);

            assertThat(result.getStockQuantity()).isEqualTo(30);
            verify(inventoryRepository).save(sampleInventory);
        }

        @Test
        @DisplayName("Should decrement stock quantity to exact zero without error")
        void adjustStock_ShouldAllowReducingStockToZero() {
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.of(sampleInventory));
            when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

            Inventory result = inventoryService.adjustStock(variantId, -20, brandId);

            assertThat(result.getStockQuantity()).isEqualTo(0);
            verify(inventoryRepository).save(sampleInventory);
        }
    }

    @Nested
    @DisplayName("deductStockInternal() Tests")
    class DeductStockInternalTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when inventory is missing")
        void deductStockInternal_ShouldThrowException_WhenInventoryNotFound() {
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> inventoryService.deductStockInternal(variantId, 5))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Inventory record not found");

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when deduction quantity exceeds available stock")
        void deductStockInternal_ShouldThrowException_WhenStockIsInsufficient() {
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.of(sampleInventory));

            assertThatThrownBy(() -> inventoryService.deductStockInternal(variantId, 25))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Insufficient stock during payment processing for variant " + variantId);

            verify(inventoryRepository, never()).save(any(Inventory.class));
        }

        @Test
        @DisplayName("Should deduct stock and save updated inventory on successful deduction")
        void deductStockInternal_ShouldDeductStockAndSave_WhenStockSufficient() {
            when(inventoryRepository.findByProductVariantId(variantId)).thenReturn(Optional.of(sampleInventory));

            inventoryService.deductStockInternal(variantId, 8);

            ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
            verify(inventoryRepository).save(captor.capture());
            assertThat(captor.getValue().getStockQuantity()).isEqualTo(12);
        }
    }
}