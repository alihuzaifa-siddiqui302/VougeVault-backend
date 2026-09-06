package com.ecommerce.VougeVault.delivery.event;

import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeliveryStatusChangedEvent extends ApplicationEvent {
    private final Delivery delivery;
    private final DeliveryStatus newStatus;

    public DeliveryStatusChangedEvent(Object source, Delivery delivery, DeliveryStatus newStatus) {
        super(source);
        this.delivery = delivery;
        this.newStatus = newStatus;
    }
}