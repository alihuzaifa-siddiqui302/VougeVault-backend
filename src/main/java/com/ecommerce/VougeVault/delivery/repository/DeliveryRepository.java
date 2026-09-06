package com.ecommerce.VougeVault.delivery.repository;

import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    Optional<Delivery> findByOrderId(Long orderId);
    List<Delivery> findByDeliveryPersonId(Long deliveryPersonId);
    List<Delivery> findByDeliveryPersonIdAndStatus(Long deliveryPersonId, DeliveryStatus status);

    // Add these methods to existing DeliveryRepository

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status = 'DELIVERED'")
    Long countDeliveredDeliveries();

    @Query("SELECT COUNT(d) FROM Delivery d WHERE d.status != 'DELIVERED' AND d.status != 'FAILED'")
    Long countInProgressDeliveries();

    @Query(value = "SELECT AVG(EXTRACT(DAY FROM (d.updated_at - o.created_at))) " +
            "FROM deliveries d JOIN orders o ON d.order_id = o.id WHERE d.status = 'DELIVERED'",
            nativeQuery = true)
    Double getAverageDeliveryDays();

    Collection<Delivery> findByDeliveryPersonIdAndStatusNot(Long deliveryPersonId, DeliveryStatus deliveryStatus);
}