package com.ecommerce.VougeVault.order.repository;

import com.ecommerce.VougeVault.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findByIdAndOrderCustomerId(Long id, Long customerId);
}
