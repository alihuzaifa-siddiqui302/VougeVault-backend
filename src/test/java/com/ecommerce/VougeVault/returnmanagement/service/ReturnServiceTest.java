package com.ecommerce.VougeVault.returnmanagement.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.email.service.EmailService;
import com.ecommerce.VougeVault.inventory.service.InventoryService;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.payment.entity.Payment;
import com.ecommerce.VougeVault.payment.repository.PaymentRepository;
import com.ecommerce.VougeVault.returnmanagement.dto.*;
import com.ecommerce.VougeVault.returnmanagement.entity.Return;
import com.ecommerce.VougeVault.returnmanagement.entity.ReturnItem;
import com.ecommerce.VougeVault.returnmanagement.entity.ReturnStatus;
import com.ecommerce.VougeVault.returnmanagement.event.ReturnApprovedEvent;
import com.ecommerce.VougeVault.returnmanagement.event.ReturnRefundedEvent;
import com.ecommerce.VougeVault.returnmanagement.event.ReturnRequestedEvent;
import com.ecommerce.VougeVault.returnmanagement.repository.ReturnItemRepository;
import com.ecommerce.VougeVault.returnmanagement.repository.ReturnRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import com.stripe.exception.ApiException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private ReturnRepository returnRepository;

    @Mock
    private ReturnItemRepository returnItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private ReturnStateMachine returnStateMachine;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReturnService returnService;

    private User sampleCustomer;
    private Brand sampleBrand;
    private Order sampleOrder;
    private OrderItem sampleOrderItem;
    private Return sampleReturn;
    private ReturnItem sampleReturnItem;
    private Payment samplePayment;
    private CreateReturnDto createReturnDto;

    private static final String STRIPE_SECRET_KEY = "sk_test_mock_secret_key";
    private final Long customerId = 1L;
    private final Long orderId = 100L;
    private final Long orderItemId = 300L;
    private final Long brandId = 5L;
    private final Long returnId = 10L;
    private final Long variantId = 50L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(returnService, "stripeSecretKey", STRIPE_SECRET_KEY);

        sampleCustomer = new User();
        sampleCustomer.setId(customerId);
        sampleCustomer.setName("Jane Doe");
        sampleCustomer.setEmail("jane@example.com");

        sampleBrand = new Brand();
        sampleBrand.setId(brandId);
        sampleBrand.setName("Vouge Apparel");

        com.ecommerce.VougeVault.catalog.entity.Product product = new com.ecommerce.VougeVault.catalog.entity.Product();
        product.setId(20L);
        product.setName("Silk Blouse");

        com.ecommerce.VougeVault.catalog.entity.ProductVariant variant = new com.ecommerce.VougeVault.catalog.entity.ProductVariant();
        variant.setId(variantId);
        variant.setProduct(product);

        sampleOrderItem = new OrderItem();
        sampleOrderItem.setId(orderItemId);
        sampleOrderItem.setProductVariant(variant);
        sampleOrderItem.setBrand(sampleBrand);
        sampleOrderItem.setQuantity(2);
        sampleOrderItem.setPriceAtPurchase(new BigDecimal("50.00"));
        sampleOrderItem.setProductNameSnapshot("Silk Blouse");

        sampleOrder = new Order();
        sampleOrder.setId(orderId);
        sampleOrder.setCustomer(sampleCustomer);
        sampleOrder.setStatus(OrderStatus.DELIVERED);
        sampleOrder.setCreatedAt(LocalDateTime.now().minusDays(5));
        sampleOrder.setItems(new ArrayList<>(List.of(sampleOrderItem)));

        sampleReturnItem = new ReturnItem();
        sampleReturnItem.setId(1000L);
        sampleReturnItem.setOrderItem(sampleOrderItem);
        sampleReturnItem.setQuantityReturned(1);
        sampleReturnItem.setRefundPerUnit(new BigDecimal("50.00"));

        sampleReturn = new Return();
        sampleReturn.setId(returnId);
        sampleReturn.setOrder(sampleOrder);
        sampleReturn.setCustomer(sampleCustomer);
        sampleReturn.setReason("Size too large");
        sampleReturn.setStatus(ReturnStatus.REQUESTED);
        sampleReturn.setRefundAmount(new BigDecimal("50.00"));
        sampleReturn.setItems(new ArrayList<>(List.of(sampleReturnItem)));
        sampleReturn.setCreatedAt(LocalDateTime.now());
        sampleReturn.setUpdatedAt(LocalDateTime.now());

        sampleReturnItem.setReturn_(sampleReturn);

        samplePayment = new Payment();
        samplePayment.setId(99L);
        samplePayment.setStripePaymentIntentId("pi_mock_12345");
        samplePayment.setOrder(sampleOrder);

        ReturnItemRequestDto itemRequestDto = new ReturnItemRequestDto();
        itemRequestDto.setOrderItemId(orderItemId);
        itemRequestDto.setQuantity(1);

        createReturnDto = new CreateReturnDto();
        createReturnDto.setOrderId(orderId);
        createReturnDto.setReason("Size too large");
        createReturnDto.setItems(List.of(itemRequestDto));
    }

    @Nested
    @DisplayName("createReturn() Tests")
    class CreateReturnTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order does not exist")
        void createReturn_ShouldThrowException_WhenOrderNotFound() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> returnService.createReturn(customerId, createReturnDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found");

            verifyNoInteractions(userRepository, returnRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when order status is not DELIVERED")
        void createReturn_ShouldThrowException_WhenOrderNotDelivered() {
            sampleOrder.setStatus(OrderStatus.SHIPPED);
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> returnService.createReturn(customerId, createReturnDto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Only delivered orders can be returned");

            verifyNoInteractions(userRepository, returnRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when return is requested after 30 days")
        void createReturn_ShouldThrowException_WhenWindowExpired() {
            sampleOrder.setCreatedAt(LocalDateTime.now().minusDays(35));
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));

            assertThatThrownBy(() -> returnService.createReturn(customerId, createReturnDto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Return window has expired (30 days from purchase)");

            verifyNoInteractions(userRepository, returnRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when customer is not found")
        void createReturn_ShouldThrowException_WhenCustomerNotFound() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> returnService.createReturn(customerId, createReturnDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Customer not found");

            verifyNoInteractions(returnRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order item does not exist in order")
        void createReturn_ShouldThrowException_WhenOrderItemNotFound() {
            createReturnDto.getItems().get(0).setOrderItemId(999L);
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

            assertThatThrownBy(() -> returnService.createReturn(customerId, createReturnDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order item not found");

            verifyNoInteractions(returnRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when return quantity exceeds ordered quantity")
        void createReturn_ShouldThrowException_WhenQuantityExceedsPurchased() {
            createReturnDto.getItems().get(0).setQuantity(5); // Ordered was 2
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

            assertThatThrownBy(() -> returnService.createReturn(customerId, createReturnDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot return more than ordered quantity");

            verifyNoInteractions(returnRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should create return, calculate total refund, publish ReturnRequestedEvent, and return DTO")
        void createReturn_ShouldCreateReturnAndPublishEvent_WhenValid() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
            when(returnRepository.save(any(Return.class))).thenAnswer(invocation -> {
                Return r = invocation.getArgument(0);
                r.setId(returnId);
                return r;
            });

            ReturnResponseDto response = returnService.createReturn(customerId, createReturnDto);

            ArgumentCaptor<Return> returnCaptor = ArgumentCaptor.forClass(Return.class);
            verify(returnRepository).save(returnCaptor.capture());
            Return savedReturn = returnCaptor.getValue();

            assertThat(savedReturn.getCustomer()).isEqualTo(sampleCustomer);
            assertThat(savedReturn.getOrder()).isEqualTo(sampleOrder);
            assertThat(savedReturn.getStatus()).isEqualTo(ReturnStatus.REQUESTED);
            assertThat(savedReturn.getReason()).isEqualTo("Size too large");
            assertThat(savedReturn.getRefundAmount()).isEqualByComparingTo("50.00"); // 50.00 * 1
            assertThat(savedReturn.getItems()).hasSize(1);

            verify(eventPublisher).publishEvent(any(ReturnRequestedEvent.class));
            assertThat(response).isNotNull();
            assertThat(response.getReturnId()).isEqualTo(returnId);
            assertThat(response.getOrderId()).isEqualTo(orderId);
            assertThat(response.getRefundAmount()).isEqualByComparingTo("50.00");
        }
    }

    @Nested
    @DisplayName("approveReturn() Tests")
    class ApproveReturnTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when return does not exist")
        void approveReturn_ShouldThrowException_WhenReturnNotFound() {
            when(returnRepository.findById(returnId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> returnService.approveReturn(returnId, brandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Return not found");

            verifyNoInteractions(returnStateMachine, eventPublisher);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when brand ID does not match order item brand")
        void approveReturn_ShouldThrowException_WhenBrandMismatch() {
            Long unauthorizedBrandId = 999L;
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));

            assertThatThrownBy(() -> returnService.approveReturn(returnId, unauthorizedBrandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Return not found");

            verifyNoInteractions(returnStateMachine, eventPublisher);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when return state machine disallows approval")
        void approveReturn_ShouldThrowException_WhenCannotApprove() {
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));
            when(returnStateMachine.canApprove(ReturnStatus.REQUESTED)).thenReturn(false);

            assertThatThrownBy(() -> returnService.approveReturn(returnId, brandId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Return cannot be approved in its current status");

            verify(returnRepository, never()).save(any(Return.class));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Should transition return to APPROVED, publish ReturnApprovedEvent, and save")
        void approveReturn_ShouldApproveAndPublishEvent_WhenValid() {
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));
            when(returnStateMachine.canApprove(ReturnStatus.REQUESTED)).thenReturn(true);
            doAnswer(invocation -> {
                Return r = invocation.getArgument(0);
                r.setStatus(ReturnStatus.APPROVED);
                return null;
            }).when(returnStateMachine).transition(sampleReturn, ReturnStatus.APPROVED);
            when(returnRepository.save(any(Return.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReturnResponseDto response = returnService.approveReturn(returnId, brandId);

            verify(returnStateMachine).transition(sampleReturn, ReturnStatus.APPROVED);
            verify(returnRepository).save(sampleReturn);
            verify(eventPublisher).publishEvent(any(ReturnApprovedEvent.class));
            assertThat(response.getStatus()).isEqualTo(ReturnStatus.APPROVED);
        }
    }

    @Nested
    @DisplayName("rejectReturn() Tests")
    class RejectReturnTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when return does not exist")
        void rejectReturn_ShouldThrowException_WhenReturnNotFound() {
            when(returnRepository.findById(returnId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> returnService.rejectReturn(returnId, brandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Return not found");

            verifyNoInteractions(returnStateMachine);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when brand does not own order")
        void rejectReturn_ShouldThrowException_WhenBrandMismatch() {
            Long unauthorizedBrandId = 999L;
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));

            assertThatThrownBy(() -> returnService.rejectReturn(returnId, unauthorizedBrandId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Return not found");

            verifyNoInteractions(returnStateMachine);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when return state machine disallows rejection")
        void rejectReturn_ShouldThrowException_WhenCannotReject() {
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));
            when(returnStateMachine.canReject(ReturnStatus.REQUESTED)).thenReturn(false);

            assertThatThrownBy(() -> returnService.rejectReturn(returnId, brandId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Return cannot be rejected in its current status");

            verify(returnRepository, never()).save(any(Return.class));
        }

        @Test
        @DisplayName("Should transition return to REJECTED and save")
        void rejectReturn_ShouldRejectAndSave_WhenValid() {
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));
            when(returnStateMachine.canReject(ReturnStatus.REQUESTED)).thenReturn(true);
            doAnswer(invocation -> {
                Return r = invocation.getArgument(0);
                r.setStatus(ReturnStatus.REJECTED);
                return null;
            }).when(returnStateMachine).transition(sampleReturn, ReturnStatus.REJECTED);
            when(returnRepository.save(any(Return.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ReturnResponseDto response = returnService.rejectReturn(returnId, brandId);

            verify(returnStateMachine).transition(sampleReturn, ReturnStatus.REJECTED);
            verify(returnRepository).save(sampleReturn);
            assertThat(response.getStatus()).isEqualTo(ReturnStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("updateReturnStatus() Tests")
    class UpdateReturnStatusTests {

        private UpdateReturnStatusDto updateDto;

        @BeforeEach
        void initUpdateDto() {
            updateDto = new UpdateReturnStatusDto();
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when return does not exist")
        void updateReturnStatus_ShouldThrowException_WhenReturnNotFound() {
            updateDto.setStatus(ReturnStatus.PICKED_UP);
            when(returnRepository.findById(returnId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> returnService.updateReturnStatus(returnId, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Return not found");

            verifyNoInteractions(returnStateMachine, inventoryService);
        }

        @Test
        @DisplayName("Should restore stock by negative quantity deduction when status is RECEIVED")
        void updateReturnStatus_ShouldRestoreStock_WhenStatusIsReceived() {
            updateDto.setStatus(ReturnStatus.RECEIVED);
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));
            when(returnRepository.save(any(Return.class))).thenAnswer(invocation -> invocation.getArgument(0));

            returnService.updateReturnStatus(returnId, updateDto);

            verify(returnStateMachine).transition(sampleReturn, ReturnStatus.RECEIVED);
            verify(inventoryService).deductStockInternal(variantId, -1);
            verify(returnRepository).save(sampleReturn);
        }

        @Test
        @DisplayName("Should execute Stripe refund flow when status is COMPLETED")
        void updateReturnStatus_ShouldTriggerStripeRefund_WhenStatusIsCompleted() {
            updateDto.setStatus(ReturnStatus.COMPLETED);
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));
            when(paymentRepository.findByStripePaymentIntentId(orderId.toString()))
                    .thenReturn(Optional.of(samplePayment));
            when(returnRepository.save(any(Return.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Refund mockRefund = mock(Refund.class);
            when(mockRefund.getId()).thenReturn("re_mock_123");

            try (MockedStatic<Refund> refundMock = mockStatic(Refund.class)) {
                refundMock.when(() -> Refund.create(any(RefundCreateParams.class))).thenReturn(mockRefund);

                ReturnResponseDto response = returnService.updateReturnStatus(returnId, updateDto);

                verify(returnStateMachine).transition(sampleReturn, ReturnStatus.COMPLETED);
                verify(returnStateMachine).transition(sampleReturn, ReturnStatus.REFUNDED);
                verify(eventPublisher).publishEvent(any(ReturnRefundedEvent.class));
                assertThat(sampleReturn.getStripeRefundId()).isEqualTo("re_mock_123");
                assertThat(response).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("initiateStripeRefund() Tests")
    class InitiateStripeRefundTests {

        @Test
        @DisplayName("Should return early without calling Stripe if payment record is missing")
        void initiateStripeRefund_ShouldReturnEarly_WhenPaymentNotFound() {
            when(paymentRepository.findByStripePaymentIntentId(orderId.toString())).thenReturn(Optional.empty());

            returnService.initiateStripeRefund(sampleReturn);

            verifyNoInteractions(returnStateMachine, eventPublisher);
            verify(returnRepository, never()).save(any(Return.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when Stripe API throws StripeException")
        void initiateStripeRefund_ShouldThrowException_WhenStripeApiFails() {
            when(paymentRepository.findByStripePaymentIntentId(orderId.toString())).thenReturn(Optional.of(samplePayment));

            try (MockedStatic<Refund> refundMock = mockStatic(Refund.class)) {
                refundMock.when(() -> Refund.create(any(RefundCreateParams.class)))
                        .thenThrow(new ApiException("Stripe API down", null, null, 500, null));

                assertThatThrownBy(() -> returnService.initiateStripeRefund(sampleReturn))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Refund failed");

                verifyNoInteractions(eventPublisher);
            }
        }

        @Test
        @DisplayName("Should create Stripe refund, transition to REFUNDED, save return, and publish event")
        void initiateStripeRefund_ShouldSucceed_WhenStripeApiReturnsRefund() {
            when(paymentRepository.findByStripePaymentIntentId(orderId.toString())).thenReturn(Optional.of(samplePayment));

            Refund mockRefund = mock(Refund.class);
            when(mockRefund.getId()).thenReturn("re_mock_987");

            try (MockedStatic<Refund> refundMock = mockStatic(Refund.class)) {
                refundMock.when(() -> Refund.create(any(RefundCreateParams.class))).thenReturn(mockRefund);

                returnService.initiateStripeRefund(sampleReturn);

                assertThat(sampleReturn.getStripeRefundId()).isEqualTo("re_mock_987");
                verify(returnStateMachine).transition(sampleReturn, ReturnStatus.REFUNDED);
                verify(returnRepository).save(sampleReturn);
                verify(eventPublisher).publishEvent(any(ReturnRefundedEvent.class));
            }
        }
    }

    @Nested
    @DisplayName("getMyReturns() & getReturn() Tests")
    class RetrieveReturnTests {

        @Test
        @DisplayName("Should return customer's return list mapped to DTOs")
        void getMyReturns_ShouldReturnMappedDtos() {
            when(returnRepository.findByCustomerId(customerId)).thenReturn(List.of(sampleReturn));

            List<ReturnResponseDto> response = returnService.getMyReturns(customerId);

            assertThat(response).hasSize(1);
            assertThat(response.get(0).getReturnId()).isEqualTo(returnId);
            assertThat(response.get(0).getOrderId()).isEqualTo(orderId);
            assertThat(response.get(0).getRefundAmount()).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when getReturn cannot find return")
        void getReturn_ShouldThrowException_WhenReturnNotFound() {
            when(returnRepository.findById(returnId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> returnService.getReturn(returnId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Return not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when customer does not own return")
        void getReturn_ShouldThrowException_WhenCustomerMismatch() {
            Long unauthorizedCustomerId = 999L;
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));

            assertThatThrownBy(() -> returnService.getReturn(returnId, unauthorizedCustomerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Return not found");
        }

        @Test
        @DisplayName("Should return single ReturnResponseDto when customer owns return")
        void getReturn_ShouldReturnDto_WhenFoundAndAuthorized() {
            when(returnRepository.findById(returnId)).thenReturn(Optional.of(sampleReturn));

            ReturnResponseDto response = returnService.getReturn(returnId, customerId);

            assertThat(response).isNotNull();
            assertThat(response.getReturnId()).isEqualTo(returnId);
            assertThat(response.getReason()).isEqualTo("Size too large");
            assertThat(response.getItems()).hasSize(1);
            assertThat(response.getItems().get(0).getProductName()).isEqualTo("Silk Blouse");
        }
    }

    @Nested
    @DisplayName("getPendingReturnsForBrand() Tests")
    class GetPendingReturnsForBrandTests {

        @Test
        @DisplayName("Should return only returns matching pending statuses and target brand ID")
        void getPendingReturnsForBrand_ShouldFilterPendingReturnsByBrand() {
            Return approvedReturn = new Return();
            approvedReturn.setId(11L);
            approvedReturn.setOrder(sampleOrder);
            approvedReturn.setStatus(ReturnStatus.APPROVED);
            approvedReturn.setItems(List.of(sampleReturnItem));
            approvedReturn.setRefundAmount(new BigDecimal("50.00"));

            Return completedReturn = new Return();
            completedReturn.setId(12L);
            completedReturn.setOrder(sampleOrder);
            completedReturn.setStatus(ReturnStatus.COMPLETED);
            completedReturn.setItems(List.of(sampleReturnItem));

            Brand otherBrand = new Brand();
            otherBrand.setId(888L);
            OrderItem otherBrandItem = new OrderItem();
            otherBrandItem.setId(400L);
            otherBrandItem.setBrand(otherBrand);
            otherBrandItem.setProductVariant(sampleOrderItem.getProductVariant());
            otherBrandItem.setProductNameSnapshot("Other Blouse");
            otherBrandItem.setPriceAtPurchase(new BigDecimal("50.00"));
            otherBrandItem.setQuantity(1);

            Order otherOrder = new Order();
            otherOrder.setId(101L);
            otherOrder.setItems(List.of(otherBrandItem));

            ReturnItem otherReturnItem = new ReturnItem();
            otherReturnItem.setId(1001L);
            otherReturnItem.setOrderItem(otherBrandItem);
            otherReturnItem.setQuantityReturned(1);
            otherReturnItem.setRefundPerUnit(new BigDecimal("50.00"));

            Return otherBrandReturn = new Return();
            otherBrandReturn.setId(13L);
            otherBrandReturn.setOrder(otherOrder);
            otherBrandReturn.setStatus(ReturnStatus.REQUESTED);
            otherBrandReturn.setItems(List.of(otherReturnItem));
            otherBrandReturn.setRefundAmount(new BigDecimal("50.00"));

            when(returnRepository.findAll()).thenReturn(List.of(
                    sampleReturn,       // REQUESTED, Brand 5 -> MATCH
                    approvedReturn,     // APPROVED,  Brand 5 -> MATCH
                    completedReturn,    // COMPLETED, Brand 5 -> EXCLUDED (status)
                    otherBrandReturn    // REQUESTED, Brand 888 -> EXCLUDED (brand)
            ));

            List<ReturnResponseDto> result = returnService.getPendingReturnsForBrand(brandId);

            assertThat(result).hasSize(2);
            assertThat(result.stream().map(ReturnResponseDto::getReturnId)).containsExactly(10L, 11L);
        }

        @Test
        @DisplayName("Should return empty list when no pending returns exist for the brand")
        void getPendingReturnsForBrand_ShouldReturnEmptyList_WhenNoneFound() {
            when(returnRepository.findAll()).thenReturn(Collections.emptyList());

            List<ReturnResponseDto> result = returnService.getPendingReturnsForBrand(brandId);

            assertThat(result).isEmpty();
        }
    }
}