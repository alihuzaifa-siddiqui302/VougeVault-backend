package com.ecommerce.VougeVault.returnmanagement.dto;

import com.ecommerce.VougeVault.returnmanagement.entity.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ReturnResponseDto {
    private Long returnId;
    private Long orderId;
    private ReturnStatus status;
    private String reason;
    private List<ReturnItemResponseDto> items;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}