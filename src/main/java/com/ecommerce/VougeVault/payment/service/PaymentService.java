package com.ecommerce.VougeVault.payment.service;

import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.inventory.service.InventoryService;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.event.OrderPaidEvent;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.order.service.OrderStateMachine;
import com.ecommerce.VougeVault.payment.dto.PaymentResponseDto;
import com.ecommerce.VougeVault.payment.entity.Payment;
import com.ecommerce.VougeVault.payment.entity.PaymentStatus;
import com.ecommerce.VougeVault.payment.repository.PaymentRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderStateMachine orderStateMachine;
    private final InventoryService inventoryService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    // ---- Step 1: customer clicks "Pay Now" ----

    @Transactional
    public PaymentResponseDto createPayment(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("Order is not in a payable state: " + order.getStatus());
        }

        long amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // DEV MOCK CHECK: Run locally without needing real Stripe API keys
        if (stripeSecretKey == null || stripeSecretKey.isBlank() || stripeSecretKey.contains("mock")) {
            log.info("==================== [DEV CONSOLE PAYMENT] ====================");
            log.info("Simulating Stripe PaymentIntent for Order #{}", order.getId());
            log.info("Amount: ₹{} ({} paise)", order.getTotalAmount(), amountInPaise);
            log.info("===============================================================");

            String mockIntentId = "pi_mock_" + order.getId() + "_" + System.currentTimeMillis();
            String mockClientSecret = mockIntentId + "_secret_mock123";

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setStripePaymentIntentId(mockIntentId);
            payment.setAmount(order.getTotalAmount());
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);

            if (order.getStatus() == OrderStatus.CREATED) {
                orderStateMachine.transition(order, OrderStatus.PAYMENT_PENDING);
                orderRepository.save(order);
            }

            return new PaymentResponseDto(
                    mockClientSecret,
                    stripePublishableKey != null ? stripePublishableKey : "pk_mock_test",
                    amountInPaise,
                    "inr"
            );
        }

        // PRODUCTION / LIVE STRIPE CALL
        com.stripe.Stripe.apiKey = stripeSecretKey;

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInPaise)
                    .setCurrency("inr")
                    .putMetadata("internal_order_id", String.valueOf(order.getId()))
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setStripePaymentIntentId(intent.getId());
            payment.setAmount(order.getTotalAmount());
            payment.setStatus(PaymentStatus.PENDING);
            paymentRepository.save(payment);

            if (order.getStatus() == OrderStatus.CREATED) {
                orderStateMachine.transition(order, OrderStatus.PAYMENT_PENDING);
                orderRepository.save(order);
            }

            return new PaymentResponseDto(
                    intent.getClientSecret(),
                    stripePublishableKey,
                    amountInPaise,
                    "inr"
            );

        } catch (StripeException e) {
            throw new RuntimeException("Failed to create Stripe payment intent: " + e.getMessage());
        }
    }

    // ---- Step 3: Stripe webhook confirms payment ----

    @Transactional
    public void handlePaymentSucceeded(String paymentIntentId) {
        boolean alreadyProcessed = paymentRepository
                .findByStripePaymentIntentIdAndStatus(paymentIntentId, PaymentStatus.SUCCESS)
                .isPresent();

        if (alreadyProcessed) {
            return; // duplicate webhook delivery — safely ignore
        }

        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        orderStateMachine.transition(order, OrderStatus.PAID);
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            ProductVariant variant = item.getProductVariant();
            inventoryService.deductStockInternal(variant.getId(), item.getQuantity());
        }
        applicationEventPublisher.publishEvent(new OrderPaidEvent(this, order));
    }

    @Transactional
    public void handlePaymentFailed(String paymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found"));

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }
}