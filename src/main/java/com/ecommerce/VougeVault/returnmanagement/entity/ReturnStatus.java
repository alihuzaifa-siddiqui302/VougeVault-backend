package com.ecommerce.VougeVault.returnmanagement.entity;

public enum ReturnStatus {
    REQUESTED,      // Customer requested return, awaiting brand approval
    APPROVED,       // Brand approved, ready for pickup
    REJECTED,       // Brand rejected the return
    PICKED_UP,      // Delivery person picked up the item
    RECEIVED,       // Brand received the returned item
    COMPLETED,      // Return fully processed, refund issued
    REFUNDED        // Refund completed in Stripe
}