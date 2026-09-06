package com.ecommerce.VougeVault.order.repository;

import com.ecommerce.VougeVault.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(Long customerId);
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    // Add these methods to your existing OrderRepository

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Order o " +
            "JOIN OrderItem oi ON o.id = oi.order.id " +
            "WHERE oi.brand.id = :brandId " +
            "AND o.status IN ('PAID', 'DELIVERED') " +
            "AND o.createdAt >= :startDate " +
            "AND o.createdAt < :endDate")
    BigDecimal getTotalRevenueForBrand(@Param("brandId") Long brandId,
                                       @Param("startDate") LocalDate startDate,
                                       @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(COUNT(o.id), 0) " +
            "FROM Order o " +
            "JOIN OrderItem oi ON o.id = oi.order.id " +
            "WHERE oi.brand.id = :brandId " +
            "AND o.status IN ('PAID', 'DELIVERED') " +
            "AND o.createdAt >= :startDate " +
            "AND o.createdAt < :endDate")
    Long getTotalOrdersForBrand(@Param("brandId") Long brandId,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(COUNT(o.id), 0) " +
            "FROM Order o " +
            "JOIN OrderItem oi ON o.id = oi.order.id " +
            "WHERE oi.brand.id = :brandId " +
            "AND o.status NOT IN ('DELIVERED', 'CANCELLED')")
    Long getActiveOrdersForBrand(@Param("brandId") Long brandId);

    @Query("SELECT o.status, COUNT(o.id) " +
            "FROM Order o " +
            "JOIN OrderItem oi ON o.id = oi.order.id " +
            "WHERE oi.brand.id = :brandId " +
            "AND o.createdAt >= :startDate " +
            "AND o.createdAt < :endDate " +
            "GROUP BY o.status")
    List<Object[]> getOrdersByStatusForBrand(@Param("brandId") Long brandId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    @Query("SELECT pv.product.id, pv.product.name, " +
            "COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0), " +
            "COALESCE(SUM(oi.quantity), 0) " +
            "FROM OrderItem oi " +
            "JOIN ProductVariant pv ON oi.productVariant.id = pv.id " +
            "JOIN Order o ON oi.order.id = o.id " +
            "WHERE oi.brand.id = :brandId " +
            "AND o.status IN ('PAID', 'DELIVERED') " +
            "AND o.createdAt >= :startDate " +
            "AND o.createdAt < :endDate " +
            "GROUP BY pv.product.id, pv.product.name " +
            "ORDER BY SUM(oi.priceAtPurchase * oi.quantity) DESC")
    List<Object[]> getTopProductsByRevenueForBrand(@Param("brandId") Long brandId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Query("SELECT pv.product.id, pv.product.name, " +
            "COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0), " +
            "COALESCE(SUM(oi.quantity), 0) " +
            "FROM OrderItem oi " +
            "JOIN ProductVariant pv ON oi.productVariant.id = pv.id " +
            "JOIN Order o ON oi.order.id = o.id " +
            "WHERE oi.brand.id = :brandId " +
            "AND o.status IN ('PAID', 'DELIVERED') " +
            "AND o.createdAt >= :startDate " +
            "AND o.createdAt < :endDate " +
            "GROUP BY pv.product.id, pv.product.name " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> getTopProductsByQuantityForBrand(@Param("brandId") Long brandId,
                                                    @Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate);

    @Query(value = "SELECT DATE(o.created_at) as date, " +
            "COALESCE(SUM(o.total_amount), 0) as revenue, " +
            "COUNT(o.id) as order_count " +
            "FROM orders o " +
            "JOIN order_items oi ON o.id = oi.order_id " +
            "WHERE oi.brand_id = :brandId " +
            "AND o.status IN ('PAID', 'DELIVERED') " +
            "AND o.created_at >= :startDate " +
            "AND o.created_at < :endDate " +
            "GROUP BY DATE(o.created_at) " +
            "ORDER BY DATE(o.created_at) ASC", nativeQuery = true)
    List<Object[]> getTrendDataForBrand(@Param("brandId") Long brandId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);


    // Add these methods to existing OrderRepository

    @Query("SELECT COUNT(DISTINCT o.id) FROM Order o WHERE o.status IN ('PAID', 'DELIVERED')")
    Long getTotalOrdersSystemWide();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN ('PAID', 'DELIVERED')")
    BigDecimal getTotalRevenueSystemWide();

    @Query("SELECT o.status, COUNT(o.id) FROM Order o GROUP BY o.status")
    List<Object[]> getOrdersByStatusSystemWide();

    @Query(value = "SELECT DATE(o.created_at) as date, COALESCE(SUM(o.total_amount), 0) as revenue, COUNT(o.id) as order_count " +
            "FROM orders o WHERE o.status IN ('PAID', 'DELIVERED') " +
            "AND o.created_at >= :startDate AND o.created_at < :endDate " +
            "GROUP BY DATE(o.created_at) ORDER BY DATE(o.created_at) ASC", nativeQuery = true)
    List<Object[]> getTrendDataSystemWide(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT b.id, b.name, COALESCE(SUM(o.totalAmount), 0), COUNT(DISTINCT o.id) " +
            "FROM Brand b LEFT JOIN Order o ON b.id IN (SELECT oi.brand.id FROM OrderItem oi WHERE oi.order.id = o.id) " +
            "WHERE o.status IN ('PAID', 'DELIVERED') " +
            "GROUP BY b.id, b.name ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> getTopBrandsSystemWide();

    @Query("SELECT pv.product.id, pv.product.name, COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0), COALESCE(SUM(oi.quantity), 0) " +
            "FROM OrderItem oi JOIN ProductVariant pv ON oi.productVariant.id = pv.id " +
            "JOIN Order o ON oi.order.id = o.id " +
            "WHERE o.status IN ('PAID', 'DELIVERED') " +
            "GROUP BY pv.product.id, pv.product.name ORDER BY SUM(oi.priceAtPurchase * oi.quantity) DESC")
    List<Object[]> getTopProductsSystemWide();
}
