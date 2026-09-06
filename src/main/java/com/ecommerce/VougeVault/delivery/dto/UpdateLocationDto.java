package com.ecommerce.VougeVault.delivery.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLocationDto {

    @NotNull(message = "Latitude is required")
    @Min(-90) @Max(90)
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required")
    @Min(-180) @Max(180)
    private BigDecimal longitude;
}