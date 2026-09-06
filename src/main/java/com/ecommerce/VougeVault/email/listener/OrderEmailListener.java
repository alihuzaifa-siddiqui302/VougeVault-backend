package com.ecommerce.VougeVault.email.listener;

import com.ecommerce.VougeVault.email.service.EmailService;
import com.ecommerce.VougeVault.order.event.OrderCreatedEvent;
import com.ecommerce.VougeVault.order.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEmailListener {

    private final EmailService emailService;

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        emailService.sendOrderConfirmedEmail(event.getOrder());
        emailService.sendNewOrderNotificationEmail(event.getOrder());
    }

    @EventListener
    public void onOrderPaid(OrderPaidEvent event) {
        emailService.sendPaymentReceivedEmail(event.getOrder());
    }
}