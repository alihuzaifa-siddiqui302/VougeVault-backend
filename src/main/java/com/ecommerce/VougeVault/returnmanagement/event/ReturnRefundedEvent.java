package com.ecommerce.VougeVault.returnmanagement.event;

import com.ecommerce.VougeVault.returnmanagement.entity.Return;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter

public class ReturnRefundedEvent extends ApplicationEvent {
    private final Return return_;

    public ReturnRefundedEvent(Object source, Return return_) {
        super(source);
        this.return_ = return_;
    }
}