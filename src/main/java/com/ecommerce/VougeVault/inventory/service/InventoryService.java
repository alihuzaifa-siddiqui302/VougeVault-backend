package com.ecommerce.VougeVault.inventory.service;

import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.catalog.Repository.ProductVariantRepository;
import com.ecommerce.VougeVault.inventory.entity.Inventory;
import com.ecommerce.VougeVault.inventory.Repository.InventoryRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductVariantRepository variantRepository;

    // ---- Brand Admin-facing methods (existing, ownership-checked) ----

    public Integer getStock(Long variantId, Long brandId) {
        confirmVariantBelongsToBrand(variantId, brandId);
        return inventoryRepository.findByProductVariantId(variantId)
                .map(Inventory::getStockQuantity)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));
    }

    @Transactional
    public Inventory adjustStock(Long variantId, Integer quantityChange, Long brandId) {
        confirmVariantBelongsToBrand(variantId, brandId);

        Inventory inventory = inventoryRepository.findByProductVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        int newQuantity = inventory.getStockQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Insufficient stock: cannot reduce below 0");
        }

        inventory.setStockQuantity(newQuantity);
        return inventoryRepository.save(inventory);
    }

    // ---- Internal, system-triggered method (no brandId check) ----

    @Transactional
    public void deductStockInternal(Long variantId, Integer quantity) {
        Inventory inventory = inventoryRepository.findByProductVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory record not found"));

        int newQuantity = inventory.getStockQuantity() - quantity;
        if (newQuantity < 0) {
            throw new IllegalStateException(
                    "Insufficient stock during payment processing for variant " + variantId
            );
        }

        inventory.setStockQuantity(newQuantity);
        inventoryRepository.save(inventory);
    }

    private void confirmVariantBelongsToBrand(Long variantId, Long brandId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        if (!variant.getProduct().getBrand().getId().equals(brandId)) {
            throw new ResourceNotFoundException("Variant not found");
        }
    }
}