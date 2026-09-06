package com.ecommerce.VougeVault.payment.controller;

import com.ecommerce.VougeVault.payment.service.PaymentService;
import com.ecommerce.VougeVault.payment.service.StripeSignatureVerifier;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeSignatureVerifier signatureVerifier;
    private final PaymentService paymentService;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String rawPayload,
            @RequestHeader("Stripe-Signature") String signatureHeader
    ) {
        Event event = signatureVerifier.verifyAndParse(rawPayload, signatureHeader);

        if (event == null) {
            return ResponseEntity.status(400).body("Invalid signature");
        }

        if ("payment_intent.succeeded".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
            paymentService.handlePaymentSucceeded(intent.getId());

        } else if ("payment_intent.payment_failed".equals(event.getType())) {
            PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer()
                    .getObject().orElseThrow();
            paymentService.handlePaymentFailed(intent.getId());
        }

        return ResponseEntity.ok("ok");
    }
}