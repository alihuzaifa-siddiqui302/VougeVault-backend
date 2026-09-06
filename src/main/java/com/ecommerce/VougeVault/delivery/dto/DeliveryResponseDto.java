package com.ecommerce.VougeVault.delivery.dto;

import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryResponseDto {
    private Long deliveryId;
    private Long orderId;
    private String deliveryPersonName;
    private DeliveryStatus status;
    private String pickupAddress;
    private String dropAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}