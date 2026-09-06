package com.ecommerce.VougeVault.review.controller;

import com.ecommerce.VougeVault.auth.model.CustomUserDetails;
import com.ecommerce.VougeVault.review.dto.*;
import com.ecommerce.VougeVault.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // Create review (customer only, after delivery)
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponseDto> createReview(
            @Valid @RequestBody CreateReviewDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(currentUser.getUserId(), dto));
    }

    // Get reviews for a product (public)
    @GetMapping("/product/{productId}")
    public ResponseEntity<ProductReviewsResponseDto> getProductReviews(
            @PathVariable Long productId
    ) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    // Get review for a specific order item (customer, ownership verified)
    @GetMapping("/order-item/{orderItemId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponseDto> getReviewForOrderItem(
            @PathVariable Long orderItemId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(reviewService.getReviewForOrderItem(orderItemId, currentUser.getUserId()));
    }

    // Get all reviews written by current customer
    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<ReviewResponseDto>> getMyReviews(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(reviewService.getMyReviews(currentUser.getUserId()));
    }

    // Update review (customer, ownership verified)
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ReviewResponseDto> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewDto dto,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(reviewService.updateReview(reviewId, currentUser.getUserId(), dto));
    }

    // Delete review (customer, ownership verified)
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        reviewService.deleteReview(reviewId, currentUser.getUserId());
        return ResponseEntity.ok(Map.of("message", "Review deleted successfully"));
    }
}
