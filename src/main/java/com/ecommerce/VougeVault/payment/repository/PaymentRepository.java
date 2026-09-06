package com.ecommerce.VougeVault.payment.repository;

import com.ecommerce.VougeVault.payment.entity.Payment;
import com.ecommerce.VougeVault.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    Optional<Payment> findByStripePaymentIntentIdAndStatus(String stripePaymentIntentId, PaymentStatus status);
}

