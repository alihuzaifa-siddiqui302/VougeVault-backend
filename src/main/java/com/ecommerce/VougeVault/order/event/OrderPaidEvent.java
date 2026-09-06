package com.ecommerce.VougeVault.order.event;

import com.ecommerce.VougeVault.order.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;

@Getter
public class OrderPaidEvent extends ApplicationEvent {

    @Autowired
    private final Order order;


    public OrderPaidEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }
}