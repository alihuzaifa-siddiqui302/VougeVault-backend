package com.ecommerce.VougeVault.catalog.Repository;

import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant,Long> {
    List<ProductVariant> findByProductId(Long productId);

    Optional<ProductVariant> findByProductIdAndSizeAndColor(Long productId, String size, String color);
    Optional<ProductVariant> findByIdAndProductId(Long id,Long productId);
}
