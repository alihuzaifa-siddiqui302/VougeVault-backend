package com.ecommerce.VougeVault.payment.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class StripeSignatureVerifier {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public Event verifyAndParse(String payload, String signatureHeader) {
        // DEV MOCK: If using a mock secret, generate a valid test signature so Webhook parses it natively
        if (webhookSecret == null || webhookSecret.isBlank() || webhookSecret.contains("mock")) {
            log.info("[DEV MOCK] Auto-signing payload to bypass signature verification in local mode");
            try {
                String autoSignedHeader = generateMockSignature(payload, webhookSecret);
                return Webhook.constructEvent(payload, autoSignedHeader, webhookSecret);
            } catch (Exception e) {
                log.error("[DEV MOCK] Failed to parse webhook payload: {}", e.getMessage());
                return null;
            }
        }

        // PRODUCTION / REAL STRIPE CLI
        try {
            return Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error verifying Stripe webhook: {}", e.getMessage());
            return null;
        }
    }

    private String generateMockSignature(String payload, String secret) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000L;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return "t=" + timestamp + ",v1=" + hex;
    }
}
