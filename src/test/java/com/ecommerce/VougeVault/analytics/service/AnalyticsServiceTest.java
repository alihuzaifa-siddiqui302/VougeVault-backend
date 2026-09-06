package com.ecommerce.VougeVault.analytics.service;

import com.ecommerce.VougeVault.analytics.dto.*;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private final Long brandId = 1L;
    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2026, 8, 1);
        endDate = LocalDate.of(2026, 8, 15);
    }

    @Nested
    @DisplayName("getDashboard() Metric Calculations")
    class DashboardMetricCalculationTests {

        @Test
        @DisplayName("Should accurately calculate revenue, orders, top products, ratings, and trend data")
        void getDashboard_ShouldCalculateAllMetricsAccurately() {
            LocalDate now = LocalDate.now();
            YearMonth currentMonth = YearMonth.now();
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();
            LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
            LocalDate weekEnd = weekStart.plusDays(7);

            // Revenue mocks
            when(orderRepository.getTotalRevenueForBrand(brandId, startDate, endDate))
                    .thenReturn(new BigDecimal("5000.00"));
            when(orderRepository.getTotalOrdersForBrand(brandId, startDate, endDate))
                    .thenReturn(100L);
            when(orderRepository.getTotalRevenueForBrand(brandId, monthStart, monthEnd.plusDays(1)))
                    .thenReturn(new BigDecimal("12000.00"));
            when(orderRepository.getTotalRevenueForBrand(brandId, weekStart, weekEnd))
                    .thenReturn(new BigDecimal("2500.00"));

            // Order metrics mocks
            when(orderRepository.getActiveOrdersForBrand(brandId)).thenReturn(12L);
            List<Object[]> ordersByStatus = List.of(
                    new Object[]{OrderStatus.PAID, 60L},
                    new Object[]{OrderStatus.CONFIRMED, 20L},
                    new Object[]{OrderStatus.DELIVERED, 20L}
            );
            when(orderRepository.getOrdersByStatusForBrand(brandId, startDate, endDate.plusDays(1)))
                    .thenReturn(ordersByStatus);

            // Top products by revenue mock
            List<Object[]> topRevenueRaw = List.of(
                    new Object[]{101L, "Silk Blouse", new BigDecimal("3000.00"), 50L},
                    new Object[]{102L, "Linen Shirt", new BigDecimal("2000.00"), 50L}
            );
            when(orderRepository.getTopProductsByRevenueForBrand(brandId, startDate, endDate.plusDays(1)))
                    .thenReturn(topRevenueRaw);

            // Top products by quantity mock
            List<Object[]> topQuantityRaw = List.of(
                    new Object[]{102L, "Linen Shirt", new BigDecimal("2000.00"), 50L},
                    new Object[]{101L, "Silk Blouse", new BigDecimal("3000.00"), 50L}
            );
            when(orderRepository.getTopProductsByQuantityForBrand(brandId, startDate, endDate.plusDays(1)))
                    .thenReturn(topQuantityRaw);

            // Review rating mocks
            when(reviewRepository.getAverageRatingForProduct(101L)).thenReturn(Optional.of(4.8));
            when(reviewRepository.getAverageRatingForProduct(102L)).thenReturn(Optional.of(4.2));

            // Trend data mock
            List<Object[]> trendRaw = List.of(
                    new Object[]{Date.valueOf(LocalDate.of(2026, 8, 1)), new BigDecimal("800.00"), 15L},
                    new Object[]{Date.valueOf(LocalDate.of(2026, 8, 2)), new BigDecimal("1200.00"), 25L}
            );
            when(orderRepository.getTrendDataForBrand(brandId, startDate, endDate.plusDays(1)))
                    .thenReturn(trendRaw);

            AnalyticsDashboardDto dashboard = analyticsService.getDashboard(brandId, startDate, endDate);

            // Revenue assertion
            RevenueMetricsDto revenue = dashboard.getRevenueMetrics();
            assertThat(revenue.getTotalRevenue()).isEqualByComparingTo("5000.00");
            assertThat(revenue.getRevenueThisMonth()).isEqualByComparingTo("12000.00");
            assertThat(revenue.getRevenueThisWeek()).isEqualByComparingTo("2500.00");
            assertThat(revenue.getAverageOrderValue()).isEqualByComparingTo("50.00"); // 5000.00 / 100

            // Order metrics assertion
            OrderMetricsDto orders = dashboard.getOrderMetrics();
            assertThat(orders.getTotalOrders()).isEqualTo(100L);
            assertThat(orders.getActiveOrders()).isEqualTo(12L);
            assertThat(orders.getOrdersByStatus()).containsEntry("PAID", 60L)
                    .containsEntry("CONFIRMED", 20L)
                    .containsEntry("DELIVERED", 20L);

            // Top products assertion
            assertThat(dashboard.getTopProductsByRevenue()).hasSize(2);
            TopProductDto topRevProduct = dashboard.getTopProductsByRevenue().get(0);
            assertThat(topRevProduct.getProductId()).isEqualTo(101L);
            assertThat(topRevProduct.getProductName()).isEqualTo("Silk Blouse");
            assertThat(topRevProduct.getRevenue()).isEqualByComparingTo("3000.00");
            assertThat(topRevProduct.getQuantity()).isEqualTo(50L);
            assertThat(topRevProduct.getAverageRating()).isEqualTo(4.8);

            assertThat(dashboard.getTopProductsByQuantity()).hasSize(2);
            assertThat(dashboard.getTopProductsByQuantity().get(0).getProductId()).isEqualTo(102L);

            // Trend data assertion
            assertThat(dashboard.getTrendData()).hasSize(2);
            assertThat(dashboard.getTrendData().get(0).getDate()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(dashboard.getTrendData().get(0).getRevenue()).isEqualByComparingTo("800.00");
            assertThat(dashboard.getTrendData().get(0).getOrderCount()).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("Date Ordering & Limits")
    class DateOrderingAndLimitsTests {

        @Test
        @DisplayName("Should swap start and end dates when startDate is chronologically after endDate")
        void getDashboard_ShouldSwapDates_WhenStartDateAfterEndDate() {
            LocalDate earlyDate = LocalDate.of(2026, 8, 1);
            LocalDate lateDate = LocalDate.of(2026, 8, 15);

            // Single stub using any() covers all 3 calls (total, month, week)
            when(orderRepository.getTotalRevenueForBrand(eq(brandId), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(BigDecimal.ZERO);
            when(orderRepository.getTotalOrdersForBrand(eq(brandId), eq(earlyDate), eq(lateDate)))
                    .thenReturn(0L);
            when(orderRepository.getActiveOrdersForBrand(brandId)).thenReturn(0L);
            when(orderRepository.getOrdersByStatusForBrand(eq(brandId), eq(earlyDate), eq(lateDate.plusDays(1))))
                    .thenReturn(Collections.emptyList());
            when(orderRepository.getTopProductsByRevenueForBrand(eq(brandId), eq(earlyDate), eq(lateDate.plusDays(1))))
                    .thenReturn(Collections.emptyList());
            when(orderRepository.getTopProductsByQuantityForBrand(eq(brandId), eq(earlyDate), eq(lateDate.plusDays(1))))
                    .thenReturn(Collections.emptyList());
            when(orderRepository.getTrendDataForBrand(eq(brandId), eq(earlyDate), eq(lateDate.plusDays(1))))
                    .thenReturn(Collections.emptyList());

            AnalyticsDashboardDto dashboard = analyticsService.getDashboard(brandId, lateDate, earlyDate);

            assertThat(dashboard.getStartDate()).isEqualTo(earlyDate);
            assertThat(dashboard.getEndDate()).isEqualTo(lateDate);
            verify(orderRepository).getTotalRevenueForBrand(brandId, earlyDate, lateDate);
            verify(orderRepository).getTrendDataForBrand(brandId, earlyDate, lateDate.plusDays(1));
        }

        @Test
        @DisplayName("Should truncate top products by revenue and quantity to maximum limit of 5")
        void getDashboard_ShouldLimitTopProductsToFive() {
            List<Object[]> sevenProducts = new ArrayList<>();
            for (long i = 1; i <= 7; i++) {
                sevenProducts.add(new Object[]{i, "Product " + i, new BigDecimal("100.00"), i * 2});
            }

            when(orderRepository.getTotalRevenueForBrand(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(orderRepository.getTotalOrdersForBrand(any(), any(), any())).thenReturn(0L);
            when(orderRepository.getActiveOrdersForBrand(brandId)).thenReturn(0L);
            when(orderRepository.getOrdersByStatusForBrand(any(), any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getTopProductsByRevenueForBrand(any(), any(), any())).thenReturn(sevenProducts);
            when(orderRepository.getTopProductsByQuantityForBrand(any(), any(), any())).thenReturn(sevenProducts);
            when(orderRepository.getTrendDataForBrand(any(), any(), any())).thenReturn(Collections.emptyList());
            when(reviewRepository.getAverageRatingForProduct(anyLong())).thenReturn(Optional.of(5.0));

            AnalyticsDashboardDto dashboard = analyticsService.getDashboard(brandId, startDate, endDate);

            assertThat(dashboard.getTopProductsByRevenue()).hasSize(5);
            assertThat(dashboard.getTopProductsByQuantity()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("Zero Divisions & Empty State Fallbacks")
    class ZeroDivisionAndFallbackTests {

        @Test
        @DisplayName("Should default averageOrderValue to BigDecimal.ZERO when totalOrders is 0")
        void getDashboard_ShouldDefaultAverageOrderValueToZero_WhenNoOrders() {
            when(orderRepository.getTotalRevenueForBrand(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(orderRepository.getTotalOrdersForBrand(any(), any(), any())).thenReturn(0L);
            when(orderRepository.getActiveOrdersForBrand(brandId)).thenReturn(0L);
            when(orderRepository.getOrdersByStatusForBrand(any(), any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getTopProductsByRevenueForBrand(any(), any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getTopProductsByQuantityForBrand(any(), any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getTrendDataForBrand(any(), any(), any())).thenReturn(Collections.emptyList());

            AnalyticsDashboardDto dashboard = analyticsService.getDashboard(brandId, startDate, endDate);

            assertThat(dashboard.getRevenueMetrics().getAverageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getOrderMetrics().getTotalOrders()).isEqualTo(0L);
            assertThat(dashboard.getOrderMetrics().getOrdersByStatus()).isEmpty();
            assertThat(dashboard.getTopProductsByRevenue()).isEmpty();
            assertThat(dashboard.getTopProductsByQuantity()).isEmpty();
            assertThat(dashboard.getTrendData()).isEmpty();
        }

        @Test
        @DisplayName("Should default product average rating to 0.0 when review rating is absent")
        void getDashboard_ShouldDefaultRatingToZero_WhenReviewRatingNotFound() {
            List<Object[]> productRow = Collections.singletonList(
                    new Object[]{105L, "Casual Tee", new BigDecimal("500.00"), 10L}
            );

            when(orderRepository.getTotalRevenueForBrand(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(orderRepository.getTotalOrdersForBrand(any(), any(), any())).thenReturn(1L);
            when(orderRepository.getActiveOrdersForBrand(brandId)).thenReturn(0L);
            when(orderRepository.getOrdersByStatusForBrand(any(), any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getTopProductsByRevenueForBrand(any(), any(), any())).thenReturn(productRow);
            when(orderRepository.getTopProductsByQuantityForBrand(any(), any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.getTrendDataForBrand(any(), any(), any())).thenReturn(Collections.emptyList());
            when(reviewRepository.getAverageRatingForProduct(105L)).thenReturn(Optional.empty());

            AnalyticsDashboardDto dashboard = analyticsService.getDashboard(brandId, startDate, endDate);

            assertThat(dashboard.getTopProductsByRevenue()).hasSize(1);
            assertThat(dashboard.getTopProductsByRevenue().get(0).getAverageRating()).isEqualTo(0.0);
        }
    }
}