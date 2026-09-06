package com.ecommerce.VougeVault.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReviewResponseDto {
    private Long reviewId;
    private Long productId;
    private String productName;
    private Integer rating;
    private String text;
    private String authorName;
    private Boolean verifiedPurchase;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}