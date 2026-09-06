package com.ecommerce.VougeVault.catalog.Repository;

import com.ecommerce.VougeVault.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product,Long> {
    List<Product> findByBrandId(Long brandId);
    Optional<Product> findByIdAndBrandId(Long id, Long brandId);
    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByCategoryIdAndBrandId(Long categoryId, Long brandId);

    List<Product> findByCategoryIdAndVariants_PriceBetween(
            Long categoryId, BigDecimal minPrice, BigDecimal maxPrice
    );

    List<Product> findByCategoryIdAndBrandIdAndVariants_PriceBetween(
            Long categoryId, Long brandId, BigDecimal minPrice, BigDecimal maxPrice
    );

    List<Product> findByBrandIdAndVariants_PriceBetween(
            Long brandId, BigDecimal minPrice, BigDecimal maxPrice
    );
}
