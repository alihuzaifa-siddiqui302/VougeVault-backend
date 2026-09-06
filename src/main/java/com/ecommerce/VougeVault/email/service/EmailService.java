package com.ecommerce.VougeVault.email.service;

import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.payment.entity.Payment;
import com.ecommerce.VougeVault.returnmanagement.entity.Return;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Content;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final SendGrid sendGrid;

    @Value("${spring.sendgrid.api-key:mock}")
    private String sendGridApiKey;

    @Value("${app.email.from:noreply@vougelvault.com}")
    private String fromEmail;

    @Value("${app.email.from-name:VougeVault}")
    private String fromName;

    // ============ ORDER EMAILS ============

    public void sendOrderConfirmedEmail(Order order) {
        String to = order.getCustomer().getEmail();
        String subject = "Order Confirmed - Order #" + order.getId();
        String htmlContent = buildOrderConfirmedTemplate(order);
        sendEmail(to, order.getCustomer().getName(), subject, htmlContent);
    }

    public void sendPaymentReceivedEmail(Order order) {
        String to = order.getCustomer().getEmail();
        String subject = "Payment Confirmed - Order #" + order.getId();
        String htmlContent = buildPaymentReceivedTemplate(order);
        sendEmail(to, order.getCustomer().getName(), subject, htmlContent);
    }

    // ============ DELIVERY EMAILS ============

    public void sendOrderShippedEmail(Delivery delivery) {
        Order order = delivery.getOrder();
        String to = order.getCustomer().getEmail();
        String subject = "Your Order is on the Way - Order #" + order.getId();
        String htmlContent = buildOrderShippedTemplate(delivery);
        sendEmail(to, order.getCustomer().getName(), subject, htmlContent);
    }

    public void sendOrderDeliveredEmail(Delivery delivery) {
        Order order = delivery.getOrder();
        String to = order.getCustomer().getEmail();
        String subject = "Order Delivered - Order #" + order.getId();
        String htmlContent = buildOrderDeliveredTemplate(delivery);
        sendEmail(to, order.getCustomer().getName(), subject, htmlContent);
    }

    public void sendDeliveryOtpEmail(Order order, String otpCode) {
        String to = order.getCustomer().getEmail();
        String subject = "Your Delivery Verification Code - Order #" + order.getId();
        String htmlContent = buildDeliveryOtpTemplate(order, otpCode);
        sendEmail(to, order.getCustomer().getName(), subject, htmlContent);
    }

    // ============ RETURN EMAILS ============

    public void sendReturnApprovedEmail(Return return_) {
        String to = return_.getCustomer().getEmail();
        String subject = "Return Approved - Order #" + return_.getOrder().getId();
        String htmlContent = buildReturnApprovedTemplate(return_);
        sendEmail(to, return_.getCustomer().getName(), subject, htmlContent);
    }

    public void sendReturnRefundedEmail(Return return_) {
        String to = return_.getCustomer().getEmail();
        String subject = "Refund Processed - Order #" + return_.getOrder().getId();
        String htmlContent = buildReturnRefundedTemplate(return_);
        sendEmail(to, return_.getCustomer().getName(), subject, htmlContent);
    }

    // ============ BRAND ADMIN EMAILS ============

    public void sendNewOrderNotificationEmail(Order order) {
        String brandEmail = order.getItems().get(0).getBrand().getEmail();

        if (brandEmail == null || brandEmail.isEmpty()) {
            log.warn("Brand has no email for order notification");
            return;
        }

        String subject = "New Order Received - Order #" + order.getId();
        String htmlContent = buildNewOrderNotificationTemplate(order);
        sendEmail(brandEmail, "Brand Admin", subject, htmlContent);
    }

    public void sendReturnRequestedEmail(Return return_) {
        String brandEmail = return_.getOrder().getItems().get(0).getBrand().getEmail();

        if (brandEmail == null || brandEmail.isEmpty()) {
            log.warn("Brand has no email for return notification");
            return;
        }

        String subject = "Return Requested - Order #" + return_.getOrder().getId();
        String htmlContent = buildReturnRequestedTemplate(return_);
        sendEmail(brandEmail, "Brand Admin", subject, htmlContent);
    }

    // ============ CORE METHOD ============

    private void sendEmail(String to, String toName, String subject, String htmlContent) {
        // Intercept emails in local/dev mode so no external API calls are made
        if (sendGridApiKey == null || sendGridApiKey.isBlank() || sendGridApiKey.contains("mock")) {
            log.info("==================== [DEV CONSOLE EMAIL] ====================");
            log.info("To: {} <{}>", toName, to);
            log.info("From: {} <{}>", fromName, fromEmail);
            log.info("Subject: {}", subject);
            log.info("Preview: {}", htmlContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim());
            log.info("=============================================================");
            return;
        }

        try {
            Email fromEmailObj = new Email(fromEmail, fromName);
            Email toEmailObj = new Email(to, toName);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(fromEmailObj, subject, toEmailObj, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() < 300) {
                log.info("Email sent successfully to {} | Subject: {}", to, subject);
            } else {
                log.error("Failed to send email to {} | Status: {} | Body: {}",
                        to, response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage());
        }
    }

    // ============ EMAIL TEMPLATES ============

    private String buildOrderConfirmedTemplate(Order order) {
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #0066cc 0%%, #0052a3 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                        .order-item { border: 1px solid #ddd; padding: 10px; margin: 10px 0; border-radius: 4px; }
                        .button { display: inline-block; background: #0066cc; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Order Confirmed! 🎉</h1>
                        </div>
                        <p>Hi %s,</p>
                        <p>Your order has been confirmed and is being prepared for shipment.</p>
                        
                        <h3>Order Details</h3>
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Order Date:</strong> %s</p>
                        <p><strong>Total Amount:</strong> ₹%.2f</p>
                        <p><strong>Delivery Address:</strong></p>
                        <p>%s<br>%s</p>
                        
                        <h3>Items (%d)</h3>
                        %s
                        
                        <p>We'll notify you as soon as your order ships. You can track your delivery in real-time.</p>
                        <a href="https://vougelvault.com/orders/%d" class="button">View Order Details</a>
                        
                        <p>Thank you for shopping with VougeVault!</p>
                        <hr>
                        <p style="font-size: 12px; color: #666;">
                            Need help? Contact us at support@vougelvault.com
                        </p>
                    </div>
                </body>
                </html>
                """,
                order.getCustomer().getName(),
                order.getId(),
                order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                order.getTotalAmount(),
                order.getDeliveryAddress(),
                order.getDeliveryCity(),
                order.getItems().size(),
                order.getItems().stream()
                        .map(item -> String.format("<div class='order-item'><strong>%s</strong> (x%d) - ₹%.2f</div>",
                                item.getProductNameSnapshot(), item.getQuantity(), item.getPriceAtPurchase()))
                        .reduce("", String::concat),
                order.getId()
        );
    }

    private String buildPaymentReceivedTemplate(Order order) {
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #28a745 0%%, #1e7e34 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Payment Received ✓</h1>
                        </div>
                        <p>Hi %s,</p>
                        <p>Your payment has been successfully processed!</p>
                        
                        <h3>Payment Details</h3>
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Amount Paid:</strong> ₹%.2f</p>
                        <p><strong>Payment Status:</strong> <span style="color: #28a745; font-weight: bold;">CONFIRMED</span></p>
                        
                        <p>Your order is now being prepared for shipment. You'll receive an email when it's out for delivery.</p>
                        
                        <p>Thank you for your purchase!</p>
                        <hr>
                        <p style="font-size: 12px; color: #666;">
                            Questions? Contact support@vougelvault.com
                        </p>
                    </div>
                </body>
                </html>
                """,
                order.getCustomer().getName(),
                order.getId(),
                order.getTotalAmount()
        );
    }

    private String buildOrderShippedTemplate(Delivery delivery) {
        Order order = delivery.getOrder();
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #ffc107 0%%, #ff9800 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                        .button { display: inline-block; background: #0066cc; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Your Order is on the Way! 🚚</h1>
                        </div>
                        <p>Hi %s,</p>
                        <p>Great news! Your order has been picked up and is now out for delivery.</p>
                        
                        <h3>Delivery Details</h3>
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Delivery Person:</strong> %s</p>
                        <p><strong>Expected Delivery:</strong> Today or tomorrow</p>
                        
                        <p>You can track your delivery in real-time to see exactly where your package is.</p>
                        
                        <a href="https://vougelvault.com/track/%d" class="button">Track Your Delivery</a>
                        
                        <p>The delivery person will call you before arrival. Make sure your phone is reachable!</p>
                        <hr>
                        <p style="font-size: 12px; color: #666;">
                            Issues? Contact support@vougelvault.com
                        </p>
                    </div>
                </body>
                </html>
                """,
                order.getCustomer().getName(),
                order.getId(),
                delivery.getDeliveryPerson() != null ? delivery.getDeliveryPerson().getUser().getName() : "Assigned",
                order.getId()
        );
    }

    private String buildOrderDeliveredTemplate(Delivery delivery) {
        Order order = delivery.getOrder();
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #28a745 0%%, #1e7e34 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                        .button { display: inline-block; background: #0066cc; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Order Delivered! 📦</h1>
                        </div>
                        <p>Hi %s,</p>
                        <p>Your order has been successfully delivered!</p>
                        
                        <h3>Order Details</h3>
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Delivered On:</strong> %s</p>
                        
                        <p>We'd love to know what you think about your purchase. Please leave a review!</p>
                        
                        <a href="https://vougelvault.com/orders/%d/review" class="button">Leave a Review</a>
                        
                        <p>Need to return something? We offer hassle-free returns within 30 days.</p>
                        
                        <p>Thank you for shopping with VougeVault!</p>
                        <hr>
                        <p style="font-size: 12px; color: #666;">
                            Any issues? Contact support@vougelvault.com
                        </p>
                    </div>
                </body>
                </html>
                """,
                order.getCustomer().getName(),
                order.getId(),
                delivery.getUpdatedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                order.getId()
        );
    }

    private String buildDeliveryOtpTemplate(Order order, String otpCode) {
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #0066cc 0%%, #0052a3 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                        .otp-code { font-size: 36px; font-weight: bold; color: #0066cc; text-align: center; letter-spacing: 4px; padding: 20px; background: #f0f0f0; border-radius: 8px; }
                        .warning { background: #fff3cd; padding: 10px; border-radius: 4px; margin: 10px 0; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Delivery Verification Code</h1>
                        </div>
                        <p>Hi %s,</p>
                        <p>Your delivery person has arrived at your location!</p>
                        
                        <p><strong>Share this 6-digit code with them to confirm delivery:</strong></p>
                        
                        <div class="otp-code">%s</div>
                        
                        <p><strong>Order ID:</strong> #%d</p>
                        
                        <div class="warning">
                            <strong>⚠️ Important:</strong> 
                            <ul>
                                <li>This code is valid for only 15 minutes</li>
                                <li>Do not share this code with anyone else</li>
                                <li>You have 3 attempts to enter the correct code</li>
                            </ul>
                        </div>
                        
                        <p>Once verified, your order will be marked as delivered.</p>
                        
                        <hr>
                        <p style="font-size: 12px; color: #666;">
                            Issues? Contact support@vougelvault.com
                        </p>
                    </div>
                </body>
                </html>
                """,
                order.getCustomer().getName(),
                otpCode,
                order.getId()
        );
    }

    private String buildReturnApprovedTemplate(Return return_) {
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #28a745 0%%, #1e7e34 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Return Approved ✓</h1>
                        </div>
                        <p>Hi %s,</p>
                        <p>Great! Your return request has been approved.</p>
                        
                        <h3>Return Details</h3>
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Refund Amount:</strong> ₹%.2f</p>
                        
                        <p>A delivery person will pick up the returned item from your location within 2-3 days.</p>
                        
                        <p>Once we receive and verify the item, your refund will be processed immediately.</p>
                        
                        <p>Thank you!</p>
                        <hr>
                        <p style="font-size: 12px; color: #666;">
                            Questions? Contact support@vougelvault.com
                        </p>
                    </div>
                </body>
                </html>
                """,
                return_.getCustomer().getName(),
                return_.getOrder().getId(),
                return_.getRefundAmount()
        );
    }

    private String buildReturnRefundedTemplate(Return return_) {
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #28a745 0%%, #1e7e34 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Refund Processed ✓</h1>
                        </div>
                        <p>Hi %s,</p>
                        <p>Your refund has been successfully processed!</p>
                        
                        <h3>Refund Details</h3>
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Refund Amount:</strong> ₹%.2f</p>
                        <p><strong>Status:</strong> <span style="color: #28a745; font-weight: bold;">COMPLETED</span></p>
                        
                        <p>The refund will appear in your account within 5-7 business days.</p>
                        
                        <p>We appreciate your business and hope to see you again!</p>
                        <hr>
                        <p style="font-size: 12px; color: #666;">
                            Issues? Contact support@vougelvault.com
                        </p>
                    </div>
                </body>
                </html>
                """,
                return_.getCustomer().getName(),
                return_.getOrder().getId(),
                return_.getRefundAmount()
        );
    }

    private String buildNewOrderNotificationTemplate(Order order) {
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #0066cc 0%%, #0052a3 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                        .button { display: inline-block; background: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>New Order Received! 🎉</h1>
                        </div>
                        <p>You have a new order!</p>
                        
                        <h3>Order Summary</h3>
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Customer:</strong> %s</p>
                        <p><strong>Total Amount:</strong> ₹%.2f</p>
                        <p><strong>Items:</strong> %d</p>
                        
                        <p>Log in to your dashboard to view order details and start processing.</p>
                        
                        <a href="https://vougelvault.com/admin/orders/%d" class="button">View Order in Dashboard</a>
                        
                        <p>Process this order quickly to provide excellent customer service!</p>
                    </div>
                </body>
                </html>
                """,
                order.getId(),
                order.getCustomer().getName(),
                order.getTotalAmount(),
                order.getItems().size(),
                order.getId()
        );
    }

    private String buildReturnRequestedTemplate(Return return_) {
        return String.format("""
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f5f5f5; }
                        .container { max-width: 600px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }
                        .header { background: linear-gradient(135deg, #ffc107 0%%, #ff9800 100%%); color: white; padding: 20px; text-align: center; border-radius: 8px; }
                        .button { display: inline-block; background: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin-top: 20px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Return Request Received</h1>
                        </div>
                        <p>You have received a return request!</p>
                        
                        <h3>Return Details</h3>
                        <p><strong>Order ID:</strong> #%d</p>
                        <p><strong>Customer:</strong> %s</p>
                        <p><strong>Reason:</strong> %s</p>
                        <p><strong>Refund Amount:</strong> ₹%.2f</p>
                        
                        <p>Log in to your dashboard to approve or reject this return.</p>
                        
                        <a href="https://vougelvault.com/admin/returns/%d" class="button">Review Return Request</a>
                        
                        <p>Respond quickly to maintain customer satisfaction!</p>
                    </div>
                </body>
                </html>
                """,
                return_.getOrder().getId(),
                return_.getCustomer().getName(),
                return_.getReason(),
                return_.getRefundAmount(),
                return_.getId()
        );
    }
}