package com.ecommerce.VougeVault.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductReviewsResponseDto {
    private Long productId;
    private String productName;
    private Double averageRating;
    private Long totalReviews;
    private List<ReviewResponseDto> reviews;
}