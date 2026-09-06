package com.ecommerce.VougeVault.order.service;

import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.CREATED, EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.PAYMENT_PENDING, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.PAID, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.REFUNDED));
        TRANSITIONS.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    public void transition(Order order, OrderStatus newStatus) {
        Set<OrderStatus> allowedNext = TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!allowedNext.contains(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition order from " + order.getStatus() + " to " + newStatus
            );
        }
        order.setStatus(newStatus);
    }

    public boolean canCancel(OrderStatus status) {
        return status == OrderStatus.CREATED
                || status == OrderStatus.PAYMENT_PENDING
                || status == OrderStatus.CONFIRMED;
    }
}