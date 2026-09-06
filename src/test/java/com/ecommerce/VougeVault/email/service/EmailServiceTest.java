package com.ecommerce.VougeVault.email.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import com.ecommerce.VougeVault.returnmanagement.entity.Return;
import com.ecommerce.VougeVault.user.entity.User;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private SendGrid sendGrid;

    @InjectMocks
    private EmailService emailService;

    private User sampleCustomer;
    private Order sampleOrder;
    private OrderItem sampleOrderItem;
    private Brand sampleBrand;
    private Delivery sampleDelivery;
    private Return sampleReturn;

    @BeforeEach
    void setUp() throws IOException {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@vougelvault.com");
        ReflectionTestUtils.setField(emailService, "fromName", "VougeVault");
        // Must be non-null and not contain "mock" to bypass console logging
        ReflectionTestUtils.setField(emailService, "sendGridApiKey", "SG.test_actual_api_key_testing");

        sampleCustomer = new User();
        sampleCustomer.setId(1L);
        sampleCustomer.setName("Alice Smith");
        sampleCustomer.setEmail("alice@example.com");

        sampleBrand = new Brand();
        sampleBrand.setId(10L);
        sampleBrand.setName("Urban Chic");
        sampleBrand.setEmail("admin@urbanchic.com");

        sampleOrderItem = new OrderItem();
        sampleOrderItem.setId(101L);
        sampleOrderItem.setProductNameSnapshot("Oversized Denim Jacket");
        sampleOrderItem.setQuantity(1);
        sampleOrderItem.setPriceAtPurchase(BigDecimal.valueOf(2499.00));
        sampleOrderItem.setBrand(sampleBrand);

        sampleOrder = new Order();
        sampleOrder.setId(501L);
        sampleOrder.setCustomer(sampleCustomer);
        sampleOrder.setTotalAmount(BigDecimal.valueOf(2499.00));
        sampleOrder.setDeliveryAddress("Flat 402, Lotus Tower");
        sampleOrder.setDeliveryCity("Mumbai");
        sampleOrder.setCreatedAt(LocalDateTime.of(2026, 8, 10, 14, 30));
        sampleOrder.setItems(List.of(sampleOrderItem));

        User deliveryUser = new User();
        deliveryUser.setName("Rajesh Courier");

        DeliveryPerson deliveryPerson = new DeliveryPerson();
        deliveryPerson.setUser(deliveryUser);

        sampleDelivery = new Delivery();
        sampleDelivery.setId(201L);
        sampleDelivery.setOrder(sampleOrder);
        sampleDelivery.setDeliveryPerson(deliveryPerson);
        sampleDelivery.setUpdatedAt(LocalDateTime.of(2026, 8, 12, 11, 15));

        sampleReturn = new Return();
        sampleReturn.setId(901L);
        sampleReturn.setOrder(sampleOrder);
        sampleReturn.setCustomer(sampleCustomer);
        sampleReturn.setReason("Size too large");
        sampleReturn.setRefundAmount(BigDecimal.valueOf(2499.00));

        Response successResponse = new Response();
        successResponse.setStatusCode(202);
        lenient().when(sendGrid.api(any(Request.class))).thenReturn(successResponse);
    }

    @Nested
    @DisplayName("Order Email Tests")
    class OrderEmailTests {

        @Test
        @DisplayName("Should build confirmed email with order items and send via SendGrid")
        void sendOrderConfirmedEmail_ShouldDispatchEmail() throws IOException {
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendOrderConfirmedEmail(sampleOrder);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getMethod()).isEqualTo(Method.POST);
            assertThat(request.getEndpoint()).isEqualTo("mail/send");
            assertThat(request.getBody()).contains("alice@example.com");
            assertThat(request.getBody()).contains("Order Confirmed - Order #501");
            assertThat(request.getBody()).contains("Oversized Denim Jacket");
        }

        @Test
        @DisplayName("Should send payment received confirmation to customer")
        void sendPaymentReceivedEmail_ShouldDispatchEmail() throws IOException {
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendPaymentReceivedEmail(sampleOrder);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getBody()).contains("alice@example.com");
            assertThat(request.getBody()).contains("Payment Confirmed - Order #501");
            assertThat(request.getBody()).contains("Payment Received");
        }
    }

    @Nested
    @DisplayName("Delivery Email Tests")
    class DeliveryEmailTests {

        @Test
        @DisplayName("Should send order shipped email with assigned courier name")
        void sendOrderShippedEmail_ShouldIncludeCourierDetails() throws IOException {
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendOrderShippedEmail(sampleDelivery);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getBody()).contains("alice@example.com");
            assertThat(request.getBody()).contains("Your Order is on the Way - Order #501");
            assertThat(request.getBody()).contains("Rajesh Courier");
        }

        @Test
        @DisplayName("Should send order delivered email upon completion")
        void sendOrderDeliveredEmail_ShouldDispatchDeliveredNotification() throws IOException {
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendOrderDeliveredEmail(sampleDelivery);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getBody()).contains("alice@example.com");
            assertThat(request.getBody()).contains("Order Delivered - Order #501");
            assertThat(request.getBody()).contains("Leave a Review");
        }

        @Test
        @DisplayName("Should send delivery OTP email with verification code")
        void sendDeliveryOtpEmail_ShouldIncludeSixDigitCode() throws IOException {
            String otpCode = "584920";
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendDeliveryOtpEmail(sampleOrder, otpCode);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getBody()).contains("alice@example.com");
            assertThat(request.getBody()).contains("Your Delivery Verification Code - Order #501");
            assertThat(request.getBody()).contains(otpCode);
        }
    }

    @Nested
    @DisplayName("Return Email Tests")
    class ReturnEmailTests {

        @Test
        @DisplayName("Should send return approved email with refund amount")
        void sendReturnApprovedEmail_ShouldDispatchApprovedNotice() throws IOException {
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendReturnApprovedEmail(sampleReturn);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getBody()).contains("alice@example.com");
            assertThat(request.getBody()).contains("Return Approved - Order #501");
            assertThat(request.getBody()).contains("2499.00");
        }

        @Test
        @DisplayName("Should send return refunded confirmation email")
        void sendReturnRefundedEmail_ShouldDispatchRefundNotice() throws IOException {
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendReturnRefundedEmail(sampleReturn);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getBody()).contains("alice@example.com");
            assertThat(request.getBody()).contains("Refund Processed - Order #501");
        }
    }

    @Nested
    @DisplayName("Brand Notification & Edge Cases")
    class BrandNotificationTests {

        @Test
        @DisplayName("Should send order notification to brand administrator")
        void sendNewOrderNotificationEmail_ShouldDispatchToBrand() throws IOException {
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendNewOrderNotificationEmail(sampleOrder);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getBody()).contains("admin@urbanchic.com");
            assertThat(request.getBody()).contains("New Order Received - Order #501");
        }

        @Test
        @DisplayName("Should bypass sending new order notification when brand email is empty")
        void sendNewOrderNotificationEmail_ShouldSkip_WhenBrandEmailEmpty() throws IOException {
            sampleBrand.setEmail("");

            emailService.sendNewOrderNotificationEmail(sampleOrder);

            verify(sendGrid, never()).api(any(Request.class));
        }

        @Test
        @DisplayName("Should send return requested email to brand administrator")
        void sendReturnRequestedEmail_ShouldDispatchToBrand() throws IOException {
            ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);

            emailService.sendReturnRequestedEmail(sampleReturn);

            verify(sendGrid).api(requestCaptor.capture());
            Request request = requestCaptor.getValue();

            assertThat(request.getBody()).contains("admin@urbanchic.com");
            assertThat(request.getBody()).contains("Return Requested - Order #501");
            assertThat(request.getBody()).contains("Size too large");
        }

        @Test
        @DisplayName("Should bypass sending return request notification when brand email is null")
        void sendReturnRequestedEmail_ShouldSkip_WhenBrandEmailNull() throws IOException {
            sampleBrand.setEmail(null);

            emailService.sendReturnRequestedEmail(sampleReturn);

            verify(sendGrid, never()).api(any(Request.class));
        }
    }

    @Nested
    @DisplayName("Failure & Resilience Handling")
    class ResilienceTests {

        @Test
        @DisplayName("Should log error without throwing exception when SendGrid returns 400 status")
        void sendEmail_ShouldNotThrow_WhenSendGridReturns4xx() throws IOException {
            Response badResponse = new Response();
            badResponse.setStatusCode(400);
            badResponse.setBody("{\"errors\":[\"Invalid email\"]}");
            when(sendGrid.api(any(Request.class))).thenReturn(badResponse);

            assertThatCode(() -> emailService.sendOrderConfirmedEmail(sampleOrder))
                    .doesNotThrowAnyException();
            verify(sendGrid).api(any(Request.class));
        }

        @Test
        @DisplayName("Should gracefully catch IOException from SendGrid client without interrupting caller")
        void sendEmail_ShouldHandleIOExceptionGracefully() throws IOException {
            when(sendGrid.api(any(Request.class))).thenThrow(new IOException("Connection timed out"));

            assertThatCode(() -> emailService.sendDeliveryOtpEmail(sampleOrder, "123456"))
                    .doesNotThrowAnyException();
            verify(sendGrid).api(any(Request.class));
        }
    }
}