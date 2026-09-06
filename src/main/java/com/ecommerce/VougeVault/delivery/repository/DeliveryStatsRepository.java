package com.ecommerce.VougeVault.delivery.repository;

import com.ecommerce.VougeVault.delivery.entity.DeliveryStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryStatsRepository extends JpaRepository<DeliveryStats, Long> {
    Optional<DeliveryStats> findByDeliveryPersonId(Long deliveryPersonId);
}