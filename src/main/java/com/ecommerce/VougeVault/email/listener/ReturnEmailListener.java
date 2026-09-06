package com.ecommerce.VougeVault.email.listener;

import com.ecommerce.VougeVault.email.service.EmailService;
import com.ecommerce.VougeVault.returnmanagement.event.ReturnRefundedEvent;
import com.ecommerce.VougeVault.returnmanagement.event.ReturnRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReturnEmailListener {

    private final EmailService emailService;

    @EventListener
    public void onReturnRefunded(ReturnRefundedEvent event) {
        emailService.sendReturnRefundedEmail(event.getReturn_());
    }

    @EventListener
    public void onReturnRequested(ReturnRequestedEvent event){
        emailService.sendReturnRequestedEmail(event.getReturn_());
    }
    @EventListener
    public void onReturnApproved(ReturnRequestedEvent event){
        emailService.sendReturnApprovedEmail(event.getReturn_());
    }

}