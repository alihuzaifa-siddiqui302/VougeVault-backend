package com.ecommerce.VougeVault.delivery.repository;

import com.ecommerce.VougeVault.delivery.entity.DeliveryOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryOtpRepository extends JpaRepository<DeliveryOtp, Long> {
    Optional<DeliveryOtp> findByDeliveryId(Long deliveryId);
}