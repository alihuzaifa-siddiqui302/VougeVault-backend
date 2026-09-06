package com.ecommerce.VougeVault.delivery.repository;

import com.ecommerce.VougeVault.delivery.entity.DeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryLocationRepository extends JpaRepository<DeliveryLocation, Long> {

    @Query(value = "SELECT * FROM delivery_locations WHERE delivery_id = :deliveryId ORDER BY created_at DESC LIMIT 1",
            nativeQuery = true)
    Optional<DeliveryLocation> findLatestByDeliveryId(@Param("deliveryId") Long deliveryId);

    @Query("SELECT dl FROM DeliveryLocation dl WHERE dl.delivery.id = :deliveryId ORDER BY dl.createdAt DESC")
    List<DeliveryLocation> findByDeliveryIdOrderByNewest(@Param("deliveryId") Long deliveryId);
}
