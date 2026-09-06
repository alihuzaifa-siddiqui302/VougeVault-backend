package com.ecommerce.VougeVault.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CheckoutRequestDto {

    @NotBlank(message = "Delivery address is required")
    @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    private String deliveryAddress;

    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100)
    private String deliveryCity;

    @NotBlank(message = "Phone number is required")
    @Size(min = 10, max = 15)
    private String deliveryPhone;

    // NEW: GPS coordinates for delivery location
    @Min(-90) @Max(90)
    private BigDecimal deliveryLatitude;

    @Min(-180) @Max(180)
    private BigDecimal deliveryLongitude;
}