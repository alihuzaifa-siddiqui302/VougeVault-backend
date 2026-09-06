package com.ecommerce.VougeVault.review.service;

import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderItemRepository;
import com.ecommerce.VougeVault.review.dto.CreateReviewDto;
import com.ecommerce.VougeVault.review.dto.ProductReviewsResponseDto;
import com.ecommerce.VougeVault.review.dto.ReviewResponseDto;
import com.ecommerce.VougeVault.review.dto.UpdateReviewDto;
import com.ecommerce.VougeVault.review.entity.Review;
import com.ecommerce.VougeVault.review.repository.ReviewRepository;
import com.ecommerce.VougeVault.shared.exception.DuplicateResourceException;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User sampleCustomer;
    private Order sampleOrder;
    private OrderItem sampleOrderItem;
    private Product sampleProduct;
    private ProductVariant sampleVariant;
    private Review sampleReview;
    private CreateReviewDto createReviewDto;
    private UpdateReviewDto updateReviewDto;

    private final Long customerId = 1L;
    private final Long orderItemId = 10L;
    private final Long productId = 50L;
    private final Long reviewId = 500L;

    @BeforeEach
    void setUp() {
        sampleCustomer = new User();
        sampleCustomer.setId(customerId);
        sampleCustomer.setName("Alice Doe");
        sampleCustomer.setEmail("alice@example.com");

        sampleProduct = new Product();
        sampleProduct.setId(productId);
        sampleProduct.setName("Silk Blouse");

        sampleVariant = new ProductVariant();
        sampleVariant.setId(100L);
        sampleVariant.setProduct(sampleProduct);

        sampleOrder = new Order();
        sampleOrder.setId(200L);
        sampleOrder.setCustomer(sampleCustomer);
        sampleOrder.setStatus(OrderStatus.DELIVERED);

        sampleOrderItem = new OrderItem();
        sampleOrderItem.setId(orderItemId);
        sampleOrderItem.setOrder(sampleOrder);
        sampleOrderItem.setProductVariant(sampleVariant);
        sampleOrderItem.setProductNameSnapshot("Silk Blouse");

        sampleReview = new Review();
        sampleReview.setId(reviewId);
        sampleReview.setOrderItem(sampleOrderItem);
        sampleReview.setCustomer(sampleCustomer);
        sampleReview.setRating(5);
        sampleReview.setText("Outstanding quality!");
        sampleReview.setVerifiedPurchase(true);
        sampleReview.setCreatedAt(LocalDateTime.now());
        sampleReview.setUpdatedAt(LocalDateTime.now());

        createReviewDto = new CreateReviewDto();
        createReviewDto.setOrderItemId(orderItemId);
        createReviewDto.setRating(5);
        createReviewDto.setText("Outstanding quality!");

        updateReviewDto = new UpdateReviewDto();
        updateReviewDto.setRating(4);
        updateReviewDto.setText("Updated: Great quality overall.");
    }

    @Nested
    @DisplayName("createReview() Tests")
    class CreateReviewTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when customer is not found")
        void createReview_ShouldThrowException_WhenCustomerNotFound() {
            when(userRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(customerId, createReviewDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Customer not found");

            verifyNoInteractions(orderItemRepository, reviewRepository);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order item does not exist for customer")
        void createReview_ShouldThrowException_WhenOrderItemNotFound() {
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
            when(orderItemRepository.findByIdAndOrderCustomerId(orderItemId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.createReview(customerId, createReviewDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order item not found");

            verifyNoInteractions(reviewRepository);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when order status is not DELIVERED")
        void createReview_ShouldThrowException_WhenOrderNotDelivered() {
            sampleOrder.setStatus(OrderStatus.SHIPPED);
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
            when(orderItemRepository.findByIdAndOrderCustomerId(orderItemId, customerId)).thenReturn(Optional.of(sampleOrderItem));

            assertThatThrownBy(() -> reviewService.createReview(customerId, createReviewDto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Order must be delivered before reviewing");

            verifyNoInteractions(reviewRepository);
        }

        @Test
        @DisplayName("Should throw DuplicateResourceException when review already exists for order item")
        void createReview_ShouldThrowException_WhenReviewAlreadyExists() {
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
            when(orderItemRepository.findByIdAndOrderCustomerId(orderItemId, customerId)).thenReturn(Optional.of(sampleOrderItem));
            when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(true);

            assertThatThrownBy(() -> reviewService.createReview(customerId, createReviewDto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("Review already exists for this order item. Please update your existing review instead.");

            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should create, save, and return review DTO when valid")
        void createReview_ShouldSaveAndReturnReview_WhenValid() {
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
            when(orderItemRepository.findByIdAndOrderCustomerId(orderItemId, customerId)).thenReturn(Optional.of(sampleOrderItem));
            when(reviewRepository.existsByOrderItemId(orderItemId)).thenReturn(false);
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
                Review r = invocation.getArgument(0);
                r.setId(reviewId);
                return r;
            });

            ReviewResponseDto response = reviewService.createReview(customerId, createReviewDto);

            ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(reviewCaptor.capture());
            Review savedReview = reviewCaptor.getValue();

            assertThat(savedReview.getOrderItem()).isEqualTo(sampleOrderItem);
            assertThat(savedReview.getCustomer()).isEqualTo(sampleCustomer);
            assertThat(savedReview.getRating()).isEqualTo(5);
            assertThat(savedReview.getText()).isEqualTo("Outstanding quality!");
            assertThat(savedReview.getVerifiedPurchase()).isTrue();

            assertThat(response).isNotNull();
            assertThat(response.getReviewId()).isEqualTo(reviewId);
            assertThat(response.getProductId()).isEqualTo(productId);
            assertThat(response.getProductName()).isEqualTo("Silk Blouse");
            assertThat(response.getAuthorName()).isEqualTo("Alice Doe");
        }
    }

    @Nested
    @DisplayName("getReviewForOrderItem() Tests")
    class GetReviewForOrderItemTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review does not exist for order item")
        void getReviewForOrderItem_ShouldThrowException_WhenReviewNotFound() {
            when(reviewRepository.findByOrderItemId(orderItemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.getReviewForOrderItem(orderItemId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Review not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when requesting customer does not own order")
        void getReviewForOrderItem_ShouldThrowException_WhenCustomerMismatch() {
            Long unauthorizedCustomerId = 999L;
            when(reviewRepository.findByOrderItemId(orderItemId)).thenReturn(Optional.of(sampleReview));

            assertThatThrownBy(() -> reviewService.getReviewForOrderItem(orderItemId, unauthorizedCustomerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Review not found");
        }

        @Test
        @DisplayName("Should return mapped ReviewResponseDto when review exists and customer owns order")
        void getReviewForOrderItem_ShouldReturnDto_WhenFoundAndAuthorized() {
            when(reviewRepository.findByOrderItemId(orderItemId)).thenReturn(Optional.of(sampleReview));

            ReviewResponseDto response = reviewService.getReviewForOrderItem(orderItemId, customerId);

            assertThat(response).isNotNull();
            assertThat(response.getReviewId()).isEqualTo(reviewId);
            assertThat(response.getRating()).isEqualTo(5);
            assertThat(response.getText()).isEqualTo("Outstanding quality!");
            assertThat(response.getAuthorName()).isEqualTo("Alice Doe");
        }
    }

    @Nested
    @DisplayName("getProductReviews() Tests")
    class GetProductReviewsTests {

        @Test
        @DisplayName("Should return empty ProductReviewsResponseDto with default metrics when no reviews exist")
        void getProductReviews_ShouldReturnDefaultDto_WhenNoReviews() {
            when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)).thenReturn(Collections.emptyList());

            ProductReviewsResponseDto response = reviewService.getProductReviews(productId);

            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(productId);
            assertThat(response.getProductName()).isEqualTo("Product");
            assertThat(response.getAverageRating()).isEqualTo(0.0);
            assertThat(response.getTotalReviews()).isEqualTo(0L);
            assertThat(response.getReviews()).isEmpty();
            verify(reviewRepository, never()).getAverageRatingForProduct(anyLong());
        }

        @Test
        @DisplayName("Should return aggregate metrics and review list when product has reviews")
        void getProductReviews_ShouldReturnMetricsAndList_WhenReviewsExist() {
            when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)).thenReturn(List.of(sampleReview));
            when(reviewRepository.getAverageRatingForProduct(productId)).thenReturn(Optional.of(4.8));
            when(reviewRepository.countReviewsForProduct(productId)).thenReturn(1L);

            ProductReviewsResponseDto response = reviewService.getProductReviews(productId);

            assertThat(response).isNotNull();
            assertThat(response.getProductId()).isEqualTo(productId);
            assertThat(response.getProductName()).isEqualTo("Silk Blouse");
            assertThat(response.getAverageRating()).isEqualTo(4.8);
            assertThat(response.getTotalReviews()).isEqualTo(1L);
            assertThat(response.getReviews()).hasSize(1);
            assertThat(response.getReviews().get(0).getReviewId()).isEqualTo(reviewId);
        }

        @Test
        @DisplayName("Should default averageRating to 0.0 when getAverageRatingForProduct returns empty")
        void getProductReviews_ShouldDefaultAverageToZero_WhenAverageRatingIsEmpty() {
            when(reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)).thenReturn(List.of(sampleReview));
            when(reviewRepository.getAverageRatingForProduct(productId)).thenReturn(Optional.empty());
            when(reviewRepository.countReviewsForProduct(productId)).thenReturn(1L);

            ProductReviewsResponseDto response = reviewService.getProductReviews(productId);

            assertThat(response.getAverageRating()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("getMyReviews() Tests")
    class GetMyReviewsTests {

        @Test
        @DisplayName("Should return customer's reviews mapped to DTOs")
        void getMyReviews_ShouldReturnCustomerReviews() {
            when(reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(List.of(sampleReview));

            List<ReviewResponseDto> response = reviewService.getMyReviews(customerId);

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getReviewId()).isEqualTo(reviewId); // or getReviewId() depending on your DTO field name
            assertThat(response.get(0).getProductName()).isEqualTo("Silk Blouse");
        }

        @Test
        @DisplayName("Should return empty list when customer has written no reviews")
        void getMyReviews_ShouldReturnEmptyList_WhenNoReviews() {
            when(reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)).thenReturn(Collections.emptyList());

            List<ReviewResponseDto> response = reviewService.getMyReviews(customerId);

            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateReview() Tests")
    class UpdateReviewTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review does not exist")
        void updateReview_ShouldThrowException_WhenReviewNotFound() {
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.updateReview(reviewId, customerId, updateReviewDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Review not found");

            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user is not the review author")
        void updateReview_ShouldThrowException_WhenUserIsNotAuthor() {
            Long unauthorizedCustomerId = 999L;
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));

            assertThatThrownBy(() -> reviewService.updateReview(reviewId, unauthorizedCustomerId, updateReviewDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Review not found");

            verify(reviewRepository, never()).save(any(Review.class));
        }

        @Test
        @DisplayName("Should update rating and text and save review when user is author")
        void updateReview_ShouldUpdateAndSave_WhenAuthorized() {
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));
            when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReviewResponseDto response = reviewService.updateReview(reviewId, customerId, updateReviewDto);

            assertThat(sampleReview.getRating()).isEqualTo(4);
            assertThat(sampleReview.getText()).isEqualTo("Updated: Great quality overall.");
            verify(reviewRepository).save(sampleReview);

            assertThat(response.getRating()).isEqualTo(4);
            assertThat(response.getText()).isEqualTo("Updated: Great quality overall.");
        }
    }

    @Nested
    @DisplayName("deleteReview() Tests")
    class DeleteReviewTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when review does not exist")
        void deleteReview_ShouldThrowException_WhenReviewNotFound() {
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reviewService.deleteReview(reviewId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Review not found");

            verify(reviewRepository, never()).delete(any(Review.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user is not the review author")
        void deleteReview_ShouldThrowException_WhenUserIsNotAuthor() {
            Long unauthorizedCustomerId = 999L;
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));

            assertThatThrownBy(() -> reviewService.deleteReview(reviewId, unauthorizedCustomerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Review not found");

            verify(reviewRepository, never()).delete(any(Review.class));
        }

        @Test
        @DisplayName("Should delete review from repository when authorized")
        void deleteReview_ShouldDelete_WhenAuthorized() {
            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(sampleReview));

            reviewService.deleteReview(reviewId, customerId);

            verify(reviewRepository).delete(sampleReview);
        }
    }
}