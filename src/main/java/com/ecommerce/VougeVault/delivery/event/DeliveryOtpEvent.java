package com.ecommerce.VougeVault.delivery.event;

import com.ecommerce.VougeVault.delivery.entity.Delivery;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeliveryOtpEvent extends ApplicationEvent {

    private final Delivery delivery;
    private final String otp;

    public DeliveryOtpEvent(Object source,Delivery delivery,String otp){
       super(source);
       this.delivery=delivery;
       this.otp=otp;
    }

}
