package com.ecommerce.VougeVault.returnmanagement.dto;

import com.ecommerce.VougeVault.returnmanagement.entity.ReturnStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateReturnStatusDto {

    @NotNull(message = "Status is required")
    private ReturnStatus status;
}