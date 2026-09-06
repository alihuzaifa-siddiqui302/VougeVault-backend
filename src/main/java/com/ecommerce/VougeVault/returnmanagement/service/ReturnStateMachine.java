package com.ecommerce.VougeVault.returnmanagement.service;

import com.ecommerce.VougeVault.returnmanagement.entity.Return;
import com.ecommerce.VougeVault.returnmanagement.entity.ReturnStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ReturnStateMachine {

    private static final Map<ReturnStatus, Set<ReturnStatus>> TRANSITIONS = new EnumMap<>(ReturnStatus.class);

    static {
        TRANSITIONS.put(ReturnStatus.REQUESTED, EnumSet.of(ReturnStatus.APPROVED, ReturnStatus.REJECTED));
        TRANSITIONS.put(ReturnStatus.APPROVED, EnumSet.of(ReturnStatus.PICKED_UP, ReturnStatus.REJECTED));
        TRANSITIONS.put(ReturnStatus.PICKED_UP, EnumSet.of(ReturnStatus.RECEIVED));
        TRANSITIONS.put(ReturnStatus.RECEIVED, EnumSet.of(ReturnStatus.COMPLETED));
        TRANSITIONS.put(ReturnStatus.COMPLETED, EnumSet.of(ReturnStatus.REFUNDED));
        TRANSITIONS.put(ReturnStatus.REFUNDED, EnumSet.noneOf(ReturnStatus.class));
        TRANSITIONS.put(ReturnStatus.REJECTED, EnumSet.noneOf(ReturnStatus.class));
    }

    public void transition(Return return_, ReturnStatus newStatus) {
        Set<ReturnStatus> allowedNext = TRANSITIONS.getOrDefault(return_.getStatus(), Set.of());
        if (!allowedNext.contains(newStatus)) {
            throw new IllegalStateException(
                    "Cannot transition return from " + return_.getStatus() + " to " + newStatus
            );
        }
        return_.setStatus(newStatus);
    }

    public boolean canApprove(ReturnStatus status) {
        return status == ReturnStatus.REQUESTED;
    }

    public boolean canReject(ReturnStatus status) {
        return status == ReturnStatus.REQUESTED || status == ReturnStatus.APPROVED;
    }
}