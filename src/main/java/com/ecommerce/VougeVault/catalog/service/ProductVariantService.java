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
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductVariant addVariant(Long productId, ProductVariantDto dto, Long brandId) {
        Product product = productRepository.findByIdAndBrandId(productId, brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        variantRepository.findByProductIdAndSizeAndColor(productId, dto.getSize(), dto.getColor())
                .ifPresent(v -> {
                    throw new DuplicateResourceException(
                            "Variant with size " + dto.getSize() + " and color " + dto.getColor() + " already exists"
                    );
                });

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSize(dto.getSize());
        variant.setColor(dto.getColor());
        variant.setPrice(dto.getPrice());
        variant.setSku(generateSku(productId, dto.getSize(), dto.getColor()));

        ProductVariant savedVariant = variantRepository.save(variant);

        Inventory inventory = new Inventory();
        inventory.setVariant(savedVariant);
        inventory.setStockQuantity(dto.getInitialStock() != null ? dto.getInitialStock() : 0);
        inventoryRepository.save(inventory);

        return savedVariant;
    }

    public List<ProductVariant> getVariantsForProduct(Long productId, Long brandId) {
        productRepository.findByIdAndBrandId(productId, brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return variantRepository.findByProductId(productId);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductVariant updateVariant(Long productId, Long variantId, ProductVariantDto dto, Long brandId) {
        productRepository.findByIdAndBrandId(productId, brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        variant.setPrice(dto.getPrice());
        return variantRepository.save(variant);
    }

    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public void deleteVariant(Long productId, Long variantId, Long brandId) {
        productRepository.findByIdAndBrandId(productId, brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductVariant variant = variantRepository.findByIdAndProductId(variantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        variantRepository.delete(variant);
    }

    private String generateSku(Long productId, String size, String color) {
        return productId + "-" + size.toUpperCase() + "-" + color.toUpperCase();
    }
}