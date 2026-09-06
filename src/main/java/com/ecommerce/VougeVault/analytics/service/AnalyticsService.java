package com.ecommerce.VougeVault.analytics.service;

import com.ecommerce.VougeVault.analytics.dto.*;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Caches the computed analytics dashboard in Redis for 10 minutes.
     * Matches the 'adminAnalytics' TTL rule configured in RedisConfig.
     */
    @Cacheable(
            value = "adminAnalytics",
            key = "{#brandId, #startDate, #endDate}"
    )
    public AnalyticsDashboardDto getDashboard(Long brandId, LocalDate startDate, LocalDate endDate) {
        // Ensure startDate is before endDate
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        // Fetch revenue metrics (safe against null SUM aggregates)
        BigDecimal totalRevenue = orderRepository.getTotalRevenueForBrand(brandId, startDate, endDate);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        Long totalOrders = orderRepository.getTotalOrdersForBrand(brandId, startDate, endDate);
        if (totalOrders == null) {
            totalOrders = 0L;
        }

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDate now = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        BigDecimal revenueThisMonth = orderRepository.getTotalRevenueForBrand(brandId, monthStart, monthEnd.plusDays(1));
        if (revenueThisMonth == null) revenueThisMonth = BigDecimal.ZERO;

        LocalDate weekStart = now.minusDays(now.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(7);
        BigDecimal revenueThisWeek = orderRepository.getTotalRevenueForBrand(brandId, weekStart, weekEnd);
        if (revenueThisWeek == null) revenueThisWeek = BigDecimal.ZERO;

        RevenueMetricsDto revenueMetrics = new RevenueMetricsDto(
                totalRevenue,
                revenueThisMonth,
                revenueThisWeek,
                averageOrderValue
        );

        // Fetch order metrics
        Long activeOrders = orderRepository.getActiveOrdersForBrand(brandId);
        List<Object[]> ordersByStatusRaw = orderRepository.getOrdersByStatusForBrand(brandId, startDate, endDate.plusDays(1));

        Map<String, Long> ordersByStatus = new HashMap<>();
        for (Object[] row : ordersByStatusRaw) {
            OrderStatus status = (OrderStatus) row[0];
            Long count = ((Number) row[1]).longValue();
            ordersByStatus.put(status.toString(), count);
        }

        OrderMetricsDto orderMetrics = new OrderMetricsDto(totalOrders, activeOrders, ordersByStatus);

        // Fetch top products by revenue
        List<Object[]> topByRevenueRaw = orderRepository.getTopProductsByRevenueForBrand(brandId, startDate, endDate.plusDays(1));
        List<TopProductDto> topProductsByRevenue = topByRevenueRaw.stream()
                .limit(5)
                .map(row -> {
                    Long productId = ((Number) row[0]).longValue();
                    String name = (String) row[1];
                    BigDecimal revenue = (BigDecimal) row[2];
                    Long quantity = ((Number) row[3]).longValue();
                    Double avgRating = reviewRepository.getAverageRatingForProduct(productId).orElse(0.0);
                    return new TopProductDto(productId, name, revenue, quantity, avgRating);
                })
                .collect(Collectors.toList());

        // Fetch top products by quantity
        List<Object[]> topByQuantityRaw = orderRepository.getTopProductsByQuantityForBrand(brandId, startDate, endDate.plusDays(1));
        List<TopProductDto> topProductsByQuantity = topByQuantityRaw.stream()
                .limit(5)
                .map(row -> {
                    Long productId = ((Number) row[0]).longValue();
                    String name = (String) row[1];
                    BigDecimal revenue = (BigDecimal) row[2];
                    Long quantity = ((Number) row[3]).longValue();
                    Double avgRating = reviewRepository.getAverageRatingForProduct(productId).orElse(0.0);
                    return new TopProductDto(productId, name, revenue, quantity, avgRating);
                })
                .collect(Collectors.toList());

        // Fetch trend data
        List<Object[]> trendRaw = orderRepository.getTrendDataForBrand(brandId, startDate, endDate.plusDays(1));
        List<TrendDataPointDto> trendData = trendRaw.stream()
                .map(row -> {
                    LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
                    BigDecimal revenue = (BigDecimal) row[1];
                    Long orderCount = ((Number) row[2]).longValue();
                    return new TrendDataPointDto(date, revenue, orderCount);
                })
                .collect(Collectors.toList());

        return new AnalyticsDashboardDto(
                startDate,
                endDate,
                revenueMetrics,
                orderMetrics,
                topProductsByRevenue,
                topProductsByQuantity,
                trendData
        );
    }
}