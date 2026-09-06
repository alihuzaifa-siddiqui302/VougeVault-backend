package com.ecommerce.VougeVault.returnmanagement.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateReturnDto {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotEmpty(message = "At least one order item must be selected for return")
    private List<ReturnItemRequestDto> items;

    @NotNull(message = "Reason is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
}