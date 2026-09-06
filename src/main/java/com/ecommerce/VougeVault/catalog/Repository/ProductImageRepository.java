package com.ecommerce.VougeVault.catalog.Repository;

import com.ecommerce.VougeVault.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    // Find all images for a product
    List<ProductImage> findByProductId(Long productId);
    // Usage: repo.findByProductId(5) → returns all images for product 5

    // Find THE primary image for a product (only one)
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.isPrimary = true")
    Optional<ProductImage> findPrimaryImageByProductId(@Param("productId") Long productId);
    // Usage: repo.findPrimaryImageByProductId(5) → returns the main image

    // Find all images, sorted by creation date
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId ORDER BY pi.createdAt ASC")
    List<ProductImage> findImagesByProductIdOrderByCreated(@Param("productId") Long productId);
    // Usage: repo.findImagesByProductIdOrderByCreated(5) → [image1, image2, image3]
}