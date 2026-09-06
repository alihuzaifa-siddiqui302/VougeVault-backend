package com.ecommerce.VougeVault.delivery.listener;

import com.ecommerce.VougeVault.delivery.service.DeliveryAssignmentService;
import com.ecommerce.VougeVault.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidListener {

    private final DeliveryAssignmentService deliveryAssignmentService;

    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Order {} paid, assigning delivery", event.getOrder().getId());
        deliveryAssignmentService.assignDeliveryForOrder(event.getOrder());
    }
}
