package com.ecommerce.VougeVault.delivery.dto;

import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDeliveryStatusDto {

    @NotNull(message = "Status is required")
    private DeliveryStatus status;
}