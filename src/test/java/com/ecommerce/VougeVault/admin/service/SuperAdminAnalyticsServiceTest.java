package com.ecommerce.VougeVault.admin.service;

import com.ecommerce.VougeVault.admin.dto.*;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.user.repository.UserRepository;
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
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminAnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private SuperAdminAnalyticsService superAdminAnalyticsService;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDate.of(2026, 8, 1);
        endDate = LocalDate.of(2026, 8, 15);
    }

    @Nested
    @DisplayName("getDashboard() Metric Calculations")
    class DashboardMetricCalculations {

        @Test
        @DisplayName("Should accurately calculate platform metrics, KPIs, brand rankings, and delivery stats")
        void getDashboard_ShouldCalculateAllMetricsAccurately() {
            // Platform revenue & orders
            when(orderRepository.getTotalRevenueSystemWide()).thenReturn(new BigDecimal("100000.00"));
            when(orderRepository.getTotalOrdersSystemWide()).thenReturn(1000L);

            // Payment success rate (800 PAID + 100 DELIVERED + 100 CANCELLED = 1000 Total, 900 Paid = 90%)
            List<Object[]> statusRows = List.of(
                    new Object[]{OrderStatus.PAID, 800L},
                    new Object[]{OrderStatus.DELIVERED, 100L},
                    new Object[]{OrderStatus.CANCELLED, 100L}
            );
            when(orderRepository.getOrdersByStatusSystemWide()).thenReturn(statusRows);

            // Top brands
            List<Object[]> topBrandsRaw = List.of(
                    new Object[]{1L, "Gucci", new BigDecimal("50000.00"), 500L},
                    new Object[]{2L, "Prada", new BigDecimal("30000.00"), 300L}
            );
            when(orderRepository.getTopBrandsSystemWide()).thenReturn(topBrandsRaw);

            // Top products
            List<Object[]> topProductsRaw = Collections.singletonList(
                    new Object[]{101L, "Silk Dress", new BigDecimal("20000.00"), 100L}
            );
            when(orderRepository.getTopProductsSystemWide()).thenReturn(topProductsRaw);

            // User metrics
            when(userRepository.countCustomers()).thenReturn(5000L);
            when(userRepository.countBrandAdmins()).thenReturn(50L);
            when(userRepository.countDeliveryPersons()).thenReturn(25L);

            // Delivery KPI mocks
            when(deliveryRepository.count()).thenReturn(180L);
            when(deliveryRepository.countDeliveredDeliveries()).thenReturn(162L);
            when(deliveryRepository.getAverageDeliveryDays()).thenReturn(2.5);

            // Trend data
            List<Object[]> trendRaw = Collections.singletonList(
                    new Object[]{Date.valueOf(LocalDate.of(2026, 8, 1)), new BigDecimal("5000.00"), 50L}
            );
            when(orderRepository.getTrendDataSystemWide(startDate, endDate.plusDays(1))).thenReturn(trendRaw);
            SuperAdminDashboardDto dashboard = superAdminAnalyticsService.getDashboard(startDate, endDate);

            // Platform assertions
            PlatformMetricsDto platform = dashboard.getPlatformMetrics();
            assertThat(platform.getTotalRevenue()).isEqualByComparingTo("100000.00");
            assertThat(platform.getTotalOrders()).isEqualTo(1000L);
            assertThat(platform.getAverageOrderValue()).isEqualByComparingTo("100.00");
            assertThat(platform.getPaymentSuccessRate()).isEqualTo(90.0);

            // Top Brands & Products assertions
            assertThat(dashboard.getTopBrands()).hasSize(2);
            assertThat(dashboard.getTopBrands().get(0).getBrandName()).isEqualTo("Gucci");
            assertThat(dashboard.getTopProducts()).hasSize(1);
            assertThat(dashboard.getTopProducts().get(0).getProductName()).isEqualTo("Silk Dress");

            // User Metrics assertions
            UserMetricsDto users = dashboard.getUserMetrics();
            assertThat(users.getTotalCustomers()).isEqualTo(5000L);
            assertThat(users.getTotalBrandAdmins()).isEqualTo(50L);
            assertThat(users.getTotalDeliveryPersons()).isEqualTo(25L);

            // Delivery KPI assertions
            DeliveryMetricsDto delivery = dashboard.getDeliveryMetrics();
            assertThat(delivery.getTotalDeliveries()).isEqualTo(180L);
            assertThat(delivery.getOnTimeDeliveries()).isEqualTo(162L);
            assertThat(delivery.getOnTimeDeliveryRate()).isEqualTo(90.0);
            assertThat(delivery.getAverageDeliveryDays()).isEqualTo(2.5);

            // Trend assertion
            assertThat(dashboard.getTrendData()).hasSize(1);
            assertThat(dashboard.getTrendData().get(0).getDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        }
    }

    @Nested
    @DisplayName("Date Ordering & Edge Cases")
    class DateOrderingAndEdgeCases {

        @Test
        @DisplayName("Should swap start and end dates when startDate is chronologically after endDate")
        void getDashboard_ShouldSwapDates_WhenStartDateAfterEndDate() {
            LocalDate earlyDate = LocalDate.of(2026, 8, 1);
            LocalDate lateDate = LocalDate.of(2026, 8, 15);

            when(orderRepository.getTotalRevenueSystemWide()).thenReturn(BigDecimal.ZERO);
            when(orderRepository.getTotalOrdersSystemWide()).thenReturn(0L);
            when(orderRepository.getOrdersByStatusSystemWide()).thenReturn(Collections.emptyList());
            when(orderRepository.getTopBrandsSystemWide()).thenReturn(Collections.emptyList());
            when(orderRepository.getTopProductsSystemWide()).thenReturn(Collections.emptyList());
            when(userRepository.countCustomers()).thenReturn(0L);
            when(userRepository.countBrandAdmins()).thenReturn(0L);
            when(userRepository.countDeliveryPersons()).thenReturn(0L);
            when(deliveryRepository.count()).thenReturn(0L);
            when(deliveryRepository.countDeliveredDeliveries()).thenReturn(0L);
            when(deliveryRepository.getAverageDeliveryDays()).thenReturn(null);
            when(orderRepository.getTrendDataSystemWide(eq(earlyDate), eq(lateDate.plusDays(1))))
                    .thenReturn(Collections.emptyList());

            SuperAdminDashboardDto dashboard = superAdminAnalyticsService.getDashboard(lateDate, earlyDate);

            assertThat(dashboard.getStartDate()).isEqualTo(earlyDate);
            assertThat(dashboard.getEndDate()).isEqualTo(lateDate);
            verify(orderRepository).getTrendDataSystemWide(earlyDate, lateDate.plusDays(1));
        }

        @Test
        @DisplayName("Should handle zero totals, empty repositories, and null averageDeliveryDays safely")
        void getDashboard_ShouldHandleZeroValuesAndNullsGracefully() {
            when(orderRepository.getTotalRevenueSystemWide()).thenReturn(BigDecimal.ZERO);
            when(orderRepository.getTotalOrdersSystemWide()).thenReturn(0L);
            when(orderRepository.getOrdersByStatusSystemWide()).thenReturn(Collections.emptyList());
            when(orderRepository.getTopBrandsSystemWide()).thenReturn(Collections.emptyList());
            when(orderRepository.getTopProductsSystemWide()).thenReturn(Collections.emptyList());
            when(userRepository.countCustomers()).thenReturn(0L);
            when(userRepository.countBrandAdmins()).thenReturn(0L);
            when(userRepository.countDeliveryPersons()).thenReturn(0L);
            when(deliveryRepository.count()).thenReturn(0L);
            when(deliveryRepository.countDeliveredDeliveries()).thenReturn(0L);
            when(deliveryRepository.getAverageDeliveryDays()).thenReturn(null);
            when(orderRepository.getTrendDataSystemWide(any(), any())).thenReturn(Collections.emptyList());

            SuperAdminDashboardDto dashboard = superAdminAnalyticsService.getDashboard(startDate, endDate);

            assertThat(dashboard.getPlatformMetrics().getAverageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getPlatformMetrics().getPaymentSuccessRate()).isEqualTo(0.0);
            assertThat(dashboard.getDeliveryMetrics().getOnTimeDeliveryRate()).isEqualTo(0.0);
            assertThat(dashboard.getDeliveryMetrics().getAverageDeliveryDays()).isEqualTo(0.0);
            assertThat(dashboard.getDeliveryMetrics().getTotalDeliveries()).isEqualTo(0L);
            assertThat(dashboard.getDeliveryMetrics().getOnTimeDeliveries()).isEqualTo(0L);
        }
    }
}