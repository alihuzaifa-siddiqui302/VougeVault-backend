package com.ecommerce.VougeVault.review.repository;

import com.ecommerce.VougeVault.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByOrderItemId(Long orderItemId);

    boolean existsByOrderItemId(Long orderItemId);

    @Query("SELECT r FROM Review r WHERE r.orderItem.productVariant.product.id = :productId ORDER BY r.createdAt DESC")
    List<Review> findByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);

    List<Review> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT AVG(CAST(r.rating AS double)) FROM Review r WHERE r.orderItem.productVariant.product.id = :productId")
    Optional<Double> getAverageRatingForProduct(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.orderItem.productVariant.product.id = :productId")
    long countReviewsForProduct(@Param("productId") Long productId);
}