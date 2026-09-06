package com.ecommerce.VougeVault.email.listener;

import com.ecommerce.VougeVault.delivery.event.DeliveryOtpEvent;
import com.ecommerce.VougeVault.delivery.event.DeliveryStatusChangedEvent;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import com.ecommerce.VougeVault.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryEmailListener {

    private final EmailService emailService;

    @EventListener
    public void onDeliveryStatusChanged(DeliveryStatusChangedEvent event) {
        if (event.getNewStatus() == DeliveryStatus.OUT_FOR_DELIVERY) {
            emailService.sendOrderShippedEmail(event.getDelivery());
        } else if (event.getNewStatus() == DeliveryStatus.DELIVERED) {
            emailService.sendOrderDeliveredEmail(event.getDelivery());
        }
    }

    @EventListener
    public void OtpGeneratingEmail(DeliveryOtpEvent event){
        emailService.sendDeliveryOtpEmail(event.getDelivery().getOrder(), event.getOtp());
    }

}