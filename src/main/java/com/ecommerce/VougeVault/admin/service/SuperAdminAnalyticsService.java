package com.ecommerce.VougeVault.admin.service;

import com.ecommerce.VougeVault.admin.dto.*;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminAnalyticsService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;

    /**
     * Caches system-wide metrics in Redis under 'adminAnalytics' (10-minute TTL).
     * Prevents running 12+ heavy database aggregations on every admin page load.
     */
    @Cacheable(
            value = "adminAnalytics",
            key = "{'superadmin', #startDate, #endDate}"
    )
    public SuperAdminDashboardDto getDashboard(LocalDate startDate, LocalDate endDate) {
        // Ensure startDate is before endDate
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        // Platform metrics with null-safe revenue check
        BigDecimal totalRevenue = orderRepository.getTotalRevenueSystemWide();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Long totalOrders = orderRepository.getTotalOrdersSystemWide();
        if (totalOrders == null) {
            totalOrders = 0L;
        }

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Payment success rate (approximation: paid orders / total orders)
        List<Object[]> ordersByStatus = orderRepository.getOrdersByStatusSystemWide();
        long paidOrders = 0L;
        long totalSystemOrders = 0L;
        for (Object[] row : ordersByStatus) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            totalSystemOrders += count;
            if (status == OrderStatus.PAID || status == OrderStatus.DELIVERED) {
                paidOrders += count;
            }
        }
        Double paymentSuccessRate = totalSystemOrders > 0 ? (paidOrders * 100.0) / totalSystemOrders : 0.0;

        PlatformMetricsDto platformMetrics = new PlatformMetricsDto(
                totalRevenue,
                totalOrders,
                averageOrderValue,
                paymentSuccessRate
        );

        // Top brands
        List<Object[]> topBrandsRaw = orderRepository.getTopBrandsSystemWide();
        List<TopBrandDto> topBrands = topBrandsRaw.stream()
                .limit(10)
                .map(row -> new TopBrandDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (BigDecimal) row[2],
                        ((Number) row[3]).longValue()
                ))
                .collect(Collectors.toList());

        // Top products
        List<Object[]> topProductsRaw = orderRepository.getTopProductsSystemWide();
        List<TopProductDto> topProducts = topProductsRaw.stream()
                .limit(10)
                .map(row -> new TopProductDto(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (BigDecimal) row[2],
                        ((Number) row[3]).longValue()
                ))
                .collect(Collectors.toList());

        // User metrics
        Long totalCustomers = userRepository.countCustomers();
        Long totalBrandAdmins = userRepository.countBrandAdmins();
        Long totalDeliveryPersons = userRepository.countDeliveryPersons();

        UserMetricsDto userMetrics = new UserMetricsDto(
                totalCustomers != null ? totalCustomers : 0L,
                totalBrandAdmins != null ? totalBrandAdmins : 0L,
                totalDeliveryPersons != null ? totalDeliveryPersons : 0L
        );

        // Delivery metrics
        Long totalDeliveries = deliveryRepository.count();
        Long onTimeDeliveries = deliveryRepository.countDeliveredDeliveries();
        Double onTimeRate = (totalDeliveries != null && totalDeliveries > 0 && onTimeDeliveries != null)
                ? (onTimeDeliveries * 100.0) / totalDeliveries
                : 0.0;

        Double avgDeliveryDays = deliveryRepository.getAverageDeliveryDays() != null
                ? deliveryRepository.getAverageDeliveryDays()
                : 0.0;

        DeliveryMetricsDto deliveryMetrics = new DeliveryMetricsDto(
                onTimeRate,
                avgDeliveryDays,
                totalDeliveries != null ? totalDeliveries : 0L,
                onTimeDeliveries != null ? onTimeDeliveries : 0L
        );

        // Trend data
        List<Object[]> trendRaw = orderRepository.getTrendDataSystemWide(startDate, endDate.plusDays(1));
        List<TrendDataPointDto> trendData = trendRaw.stream()
                .map(row -> new TrendDataPointDto(
                        ((java.sql.Date) row[0]).toLocalDate(),
                        (BigDecimal) row[1],
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());

        return new SuperAdminDashboardDto(
                startDate,
                endDate,
                platformMetrics,
                topBrands,
                topProducts,
                userMetrics,
                deliveryMetrics,
                trendData
        );
    }
}