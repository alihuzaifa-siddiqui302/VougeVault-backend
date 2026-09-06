package com.ecommerce.VougeVault.review.service;

import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderItemRepository;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.review.dto.*;
import com.ecommerce.VougeVault.review.entity.Review;
import com.ecommerce.VougeVault.review.repository.ReviewRepository;
import com.ecommerce.VougeVault.shared.exception.DuplicateResourceException;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponseDto createReview(Long customerId, CreateReviewDto dto) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Fetch the OrderItem

        OrderItem orderItem = orderItemRepository.findByIdAndOrderCustomerId(dto.getOrderItemId(), customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

// Verify order is delivered
        if (orderItem.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order must be delivered before reviewing");
        }



        // Check for duplicate review
        if (reviewRepository.existsByOrderItemId(dto.getOrderItemId())) {
            throw new DuplicateResourceException("Review already exists for this order item. Please update your existing review instead.");
        }

        Review review = new Review();
        review.setOrderItem(orderItem);
        review.setCustomer(customer);
        review.setRating(dto.getRating());
        review.setText(dto.getText());
        review.setVerifiedPurchase(true);

        review = reviewRepository.save(review);
        return toResponseDto(review);
    }

    public ReviewResponseDto getReviewForOrderItem(Long orderItemId, Long customerId) {
        Review review = reviewRepository.findByOrderItemId(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Verify customer owns the order
        if (!review.getOrderItem().getOrder().getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Review not found");
        }

        return toResponseDto(review);
    }

    public ProductReviewsResponseDto getProductReviews(Long productId) {
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);

        if (reviews.isEmpty()) {
            return new ProductReviewsResponseDto(
                    productId,
                    "Product",
                    0.0,
                    0L,
                    List.of()
            );
        }

        Double averageRating = reviewRepository.getAverageRatingForProduct(productId).orElse(0.0);
        long totalReviews = reviewRepository.countReviewsForProduct(productId);

        String productName = reviews.get(0).getOrderItem().getProductNameSnapshot();

        List<ReviewResponseDto> reviewDtos = reviews.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());

        return new ProductReviewsResponseDto(
                productId,
                productName,
                averageRating,
                totalReviews,
                reviewDtos
        );
    }

    public List<ReviewResponseDto> getMyReviews(Long customerId) {
        return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponseDto updateReview(Long reviewId, Long customerId, UpdateReviewDto dto) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Verify ownership
        if (!review.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Review not found");
        }

        review.setRating(dto.getRating());
        review.setText(dto.getText());
        review = reviewRepository.save(review);

        return toResponseDto(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long customerId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        // Verify ownership
        if (!review.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Review not found");
        }

        reviewRepository.delete(review);
    }

    private ReviewResponseDto toResponseDto(Review review) {
        OrderItem orderItem = review.getOrderItem();
        return new ReviewResponseDto(
                review.getId(),
                orderItem.getProductVariant().getProduct().getId(),
                orderItem.getProductNameSnapshot(),
                review.getRating(),
                review.getText(),
                review.getCustomer().getName(),
                review.getVerifiedPurchase(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
