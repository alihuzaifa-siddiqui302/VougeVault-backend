package com.ecommerce.VougeVault.returnmanagement.repository;

import com.ecommerce.VougeVault.returnmanagement.entity.Return;
import com.ecommerce.VougeVault.returnmanagement.entity.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReturnRepository extends JpaRepository<Return, Long> {
    Optional<Return> findByOrderId(Long orderId);
    List<Return> findByCustomerId(Long customerId);
    List<Return> findByOrderIdAndStatus(Long orderId, ReturnStatus status);
    List<Return> findByStatus(ReturnStatus status);
}