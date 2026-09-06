package com.ecommerce.VougeVault.payment.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class StripeSignatureVerifierTest {

    @InjectMocks
    private StripeSignatureVerifier verifier;

    private static final String WEBHOOK_SECRET = "whsec_test_secret_123456789";
    private static final String PAYLOAD = "{\"id\":\"evt_test_123\",\"type\":\"payment_intent.succeeded\"}";
    private static final String SIGNATURE_HEADER = "t=1492774577,v1=5257a869e7ecebeda32affa62cd493d83ece730f1ee517f7980b3882f50bedc3";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(verifier, "webhookSecret", WEBHOOK_SECRET);
    }

    @Test
    @DisplayName("Should return parsed Event when Stripe webhook signature is valid")
    void verifyAndParse_ShouldReturnEvent_WhenSignatureIsValid() throws Exception {
        Event mockEvent = mock(Event.class);

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE_HEADER, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            Event result = verifier.verifyAndParse(PAYLOAD, SIGNATURE_HEADER);

            assertThat(result).isNotNull();
            assertThat(result).isSameAs(mockEvent);
        }
    }

    @Test
    @DisplayName("Should return null when SignatureVerificationException is thrown")
    void verifyAndParse_ShouldReturnNull_WhenSignatureVerificationFails() {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            webhookMock.when(() -> Webhook.constructEvent(PAYLOAD, SIGNATURE_HEADER, WEBHOOK_SECRET))
                    .thenThrow(new SignatureVerificationException("Invalid signature", "sig_header"));

            Event result = verifier.verifyAndParse(PAYLOAD, SIGNATURE_HEADER);

            assertThat(result).isNull();
        }
    }
}