package com.ecommerce.VougeVault.returnmanagement.event;

import com.ecommerce.VougeVault.returnmanagement.entity.Return;
import lombok.Getter;

@Getter
public class ReturnApprovedEvent {
    private final Return return_;

    public ReturnApprovedEvent(Object source, Return return_) {
        super();
        this.return_ = return_;
    }
}
