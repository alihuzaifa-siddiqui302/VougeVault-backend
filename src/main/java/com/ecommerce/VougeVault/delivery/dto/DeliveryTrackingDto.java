package com.ecommerce.VougeVault.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeliveryTrackingDto {
    private Long deliveryId;
    private Long orderId;
    private String deliveryPersonName;
    private String deliveryStatus;
    private DeliveryLocationDto currentLocation;
    private Double distanceToDestinationKm;
    private Integer estimatedMinutesToArrive;
    private String estimatedDeliveryTime;  // "Today by 5:00 PM"
}