package com.ecommerce.VougeVault.returnmanagement.entity;

import com.ecommerce.VougeVault.order.entity.OrderItem;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "return_items")
@Getter
@Setter
public class ReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_id", nullable = false)
    private Return return_;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantityReturned;

    @Column(name = "refund_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal refundPerUnit;
}