package com.ecommerce.VougeVault.order.dto;

import com.ecommerce.VougeVault.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class OrderResponseDto {

    private Long OrderId;
    private OrderStatus status;
    private BigDecimal totalAmount;
   private LocalDateTime createdAt;
    private List<OrderItemResponseDto> items;

}
