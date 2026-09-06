package com.ecommerce.VougeVault.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class PaymentResponseDto {
    private String clientSecret;        // frontend uses this with Stripe.js to collect payment
    private String publishableKey;      // frontend also needs this to initialize Stripe.js
    private Long amountInPaise;
    private String currency;
}
