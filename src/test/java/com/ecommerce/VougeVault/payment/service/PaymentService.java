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
import com.stripe.exception.ApiException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStateMachine orderStateMachine;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private PaymentService paymentService;

    private Order sampleOrder;
    private OrderItem sampleOrderItem;
    private ProductVariant sampleVariant;
    private Payment samplePayment;

    // Keys MUST NOT contain "mock" to exercise the real Stripe SDK branch
    private static final String STRIPE_SECRET_KEY = "sk_test_actual_test_key";
    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_actual_pub_key";
    private static final String PAYMENT_INTENT_ID = "pi_3MtwBwLkdIwHu7ix28a3tqPa";
    private static final String CLIENT_SECRET = "pi_3MtwBwLkdIwHu7ix28a3tqPa_secret_test";
    private final Long orderId = 100L;
    private final Long customerId = 1L;
    private final Long variantId = 50L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "stripeSecretKey", STRIPE_SECRET_KEY);
        ReflectionTestUtils.setField(paymentService, "stripePublishableKey", STRIPE_PUBLISHABLE_KEY);

        sampleVariant = new ProductVariant();
        sampleVariant.setId(variantId);

        sampleOrderItem = new OrderItem();
        sampleOrderItem.setId(200L);
        sampleOrderItem.setProductVariant(sampleVariant);
        sampleOrderItem.setQuantity(2);

        sampleOrder = new Order();
        sampleOrder.setId(orderId);
        sampleOrder.setStatus(OrderStatus.CREATED);
        sampleOrder.setTotalAmount(new BigDecimal("150.00"));
        sampleOrder.setItems(List.of(sampleOrderItem));

        samplePayment = new Payment();
        samplePayment.setId(10L);
        samplePayment.setOrder(sampleOrder);
        samplePayment.setStripePaymentIntentId(PAYMENT_INTENT_ID);
        samplePayment.setAmount(new BigDecimal("150.00"));
        samplePayment.setStatus(PaymentStatus.PENDING);
    }

    @Nested
    @DisplayName("createPayment() Tests")
    class CreatePaymentTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order does not exist")
        void createPayment_ShouldThrowException_WhenOrderNotFound() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.createPayment(orderId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found");

            verifyNoInteractions(paymentRepository, orderStateMachine);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when order is not in CREATED or PAYMENT_PENDING state")
        void createPayment_ShouldThrowException_WhenOrderInInvalidState() {
            sampleOrder.setStatus(OrderStatus.CONFIRMED);
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> paymentService.createPayment(orderId, customerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Order is not in a payable state: CONFIRMED");

            verifyNoInteractions(paymentRepository, orderStateMachine);
        }

        @Test
        @DisplayName("Should throw RuntimeException when Stripe API call throws StripeException")
        void createPayment_ShouldThrowException_WhenStripeApiFails() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));

            try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
                paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                        .thenThrow(new ApiException("Stripe API down", null, null, 500, null));

                assertThatThrownBy(() -> paymentService.createPayment(orderId, customerId))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Failed to create Stripe payment intent");

                verify(paymentRepository, never()).save(any(Payment.class));
            }
        }

        @Test
        @DisplayName("Should create payment intent, save payment, transition order, and return PaymentResponseDto when order is CREATED")
        void createPayment_ShouldSucceed_WhenOrderIsCreated() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));

            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn(PAYMENT_INTENT_ID);
            when(mockIntent.getClientSecret()).thenReturn(CLIENT_SECRET);

            try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
                paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                        .thenReturn(mockIntent);

                PaymentResponseDto response = paymentService.createPayment(orderId, customerId);

                // Verify Stripe Payment parameters & persistence
                ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
                verify(paymentRepository).save(paymentCaptor.capture());
                Payment savedPayment = paymentCaptor.getValue();

                assertThat(savedPayment.getOrder()).isEqualTo(sampleOrder);
                assertThat(savedPayment.getStripePaymentIntentId()).isEqualTo(PAYMENT_INTENT_ID);
                assertThat(savedPayment.getAmount()).isEqualByComparingTo("150.00");
                assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);

                // Verify order state transition
                verify(orderStateMachine).transition(sampleOrder, OrderStatus.PAYMENT_PENDING);
                verify(orderRepository).save(sampleOrder);

                // Verify response DTO
                assertThat(response.getClientSecret()).isEqualTo(CLIENT_SECRET);
                assertThat(response.getPublishableKey()).isEqualTo(STRIPE_PUBLISHABLE_KEY);
                assertThat(response.getAmountInPaise()).isEqualTo(15000L); // 150.00 * 100
                assertThat(response.getCurrency()).isEqualTo("inr");
            }
        }

        @Test
        @DisplayName("Should not re-transition order status if order is already PAYMENT_PENDING")
        void createPayment_ShouldNotTransitionOrder_WhenOrderIsAlreadyPaymentPending() {
            sampleOrder.setStatus(OrderStatus.PAYMENT_PENDING);
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));

            PaymentIntent mockIntent = mock(PaymentIntent.class);
            when(mockIntent.getId()).thenReturn(PAYMENT_INTENT_ID);
            when(mockIntent.getClientSecret()).thenReturn(CLIENT_SECRET);

            try (MockedStatic<PaymentIntent> paymentIntentMock = mockStatic(PaymentIntent.class)) {
                paymentIntentMock.when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
                        .thenReturn(mockIntent);

                PaymentResponseDto response = paymentService.createPayment(orderId, customerId);

                verify(orderStateMachine, never()).transition(any(), any());
                verify(orderRepository, never()).save(sampleOrder);
                assertThat(response.getClientSecret()).isEqualTo(CLIENT_SECRET);
            }
        }
    }

    @Nested
    @DisplayName("handlePaymentSucceeded() Tests")
    class HandlePaymentSucceededTests {

        @Test
        @DisplayName("Should ignore duplicate webhook delivery if payment is already marked SUCCESS")
        void handlePaymentSucceeded_ShouldReturnEarly_WhenAlreadyProcessed() {
            when(paymentRepository.findByStripePaymentIntentIdAndStatus(PAYMENT_INTENT_ID, PaymentStatus.SUCCESS))
                    .thenReturn(Optional.of(samplePayment));

            paymentService.handlePaymentSucceeded(PAYMENT_INTENT_ID);

            verify(paymentRepository, never()).findByStripePaymentIntentId(anyString());
            verify(paymentRepository, never()).save(any(Payment.class));
            verifyNoInteractions(orderStateMachine, inventoryService, applicationEventPublisher);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when payment record does not exist")
        void handlePaymentSucceeded_ShouldThrowException_WhenPaymentNotFound() {
            when(paymentRepository.findByStripePaymentIntentIdAndStatus(PAYMENT_INTENT_ID, PaymentStatus.SUCCESS))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.handlePaymentSucceeded(PAYMENT_INTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Payment record not found");

            verifyNoInteractions(orderStateMachine, inventoryService, applicationEventPublisher);
        }

        @Test
        @DisplayName("Should update payment status, transition order to PAID, deduct inventory, and publish event")
        void handlePaymentSucceeded_ShouldProcessPaymentAndOrder_WhenValid() {
            when(paymentRepository.findByStripePaymentIntentIdAndStatus(PAYMENT_INTENT_ID, PaymentStatus.SUCCESS))
                    .thenReturn(Optional.empty());
            when(paymentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID))
                    .thenReturn(Optional.of(samplePayment));

            paymentService.handlePaymentSucceeded(PAYMENT_INTENT_ID);

            // Payment verification
            assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentRepository).save(samplePayment);

            // Order transition verification
            verify(orderStateMachine).transition(sampleOrder, OrderStatus.PAID);
            verify(orderRepository).save(sampleOrder);

            // Inventory deduction verification
            verify(inventoryService).deductStockInternal(variantId, 2);

            // Event publishing verification
            verify(applicationEventPublisher).publishEvent(any(OrderPaidEvent.class));
        }
    }

    @Nested
    @DisplayName("handlePaymentFailed() Tests")
    class HandlePaymentFailedTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when payment record does not exist")
        void handlePaymentFailed_ShouldThrowException_WhenPaymentNotFound() {
            when(paymentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.handlePaymentFailed(PAYMENT_INTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Payment record not found");

            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should mark payment status as FAILED and save")
        void handlePaymentFailed_ShouldUpdateStatusToFailed_WhenFound() {
            when(paymentRepository.findByStripePaymentIntentId(PAYMENT_INTENT_ID)).thenReturn(Optional.of(samplePayment));

            paymentService.handlePaymentFailed(PAYMENT_INTENT_ID);

            assertThat(samplePayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(paymentRepository).save(samplePayment);
        }
    }
}