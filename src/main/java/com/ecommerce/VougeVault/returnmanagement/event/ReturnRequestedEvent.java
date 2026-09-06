package com.ecommerce.VougeVault.returnmanagement.event;

import com.ecommerce.VougeVault.returnmanagement.entity.Return;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter

public class ReturnRequestedEvent extends ApplicationEvent {
    private final Return return_;

    public ReturnRequestedEvent(Object source, Return return_) {
        super(source);
        this.return_ = return_;
    }
}
