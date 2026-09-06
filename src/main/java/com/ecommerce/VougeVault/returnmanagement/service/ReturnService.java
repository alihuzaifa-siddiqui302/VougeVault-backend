package com.ecommerce.VougeVault.returnmanagement.service;

import com.ecommerce.VougeVault.email.service.EmailService;
import com.ecommerce.VougeVault.inventory.service.InventoryService;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
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
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnService {

    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;
    private final ReturnStateMachine returnStateMachine;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EmailService emailService;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    // ---- Customer: Create return request ----

    @Transactional
    public ReturnResponseDto createReturn(Long customerId, CreateReturnDto dto) {
        Order order = orderRepository.findByIdAndCustomerId(dto.getOrderId(), customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Verify order is delivered
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Only delivered orders can be returned");
        }

        // Verify return is within 30 days
        if (order.getCreatedAt().toLocalDate().isBefore(LocalDate.now().minusDays(30))) {
            throw new IllegalStateException("Return window has expired (30 days from purchase)");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Return return_ = new Return();
        return_.setOrder(order);
        return_.setCustomer(customer);
        return_.setReason(dto.getReason());
        return_.setStatus(ReturnStatus.REQUESTED);
        return_.setItems(new ArrayList<>());

        BigDecimal totalRefund = BigDecimal.ZERO;

        for (ReturnItemRequestDto itemDto : dto.getItems()) {
            OrderItem orderItem = order.getItems().stream()
                    .filter(item -> item.getId().equals(itemDto.getOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Order item not found"));

            if (itemDto.getQuantity() > orderItem.getQuantity()) {
                throw new IllegalArgumentException("Cannot return more than ordered quantity");
            }

            ReturnItem returnItem = new ReturnItem();
            returnItem.setReturn_(return_);
            returnItem.setOrderItem(orderItem);
            returnItem.setQuantityReturned(itemDto.getQuantity());
            returnItem.setRefundPerUnit(orderItem.getPriceAtPurchase());

            BigDecimal itemRefund = orderItem.getPriceAtPurchase()
                    .multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalRefund = totalRefund.add(itemRefund);

            return_.getItems().add(returnItem);
        }

        return_.setRefundAmount(totalRefund);
        return_ = returnRepository.save(return_);

        log.info("Return request created: {} for order {}", return_.getId(), order.getId());

        // After creating return
        eventPublisher.publishEvent(new ReturnRequestedEvent(this, return_));


        return toResponseDto(return_);

    }

    // ---- Brand Admin: Approve/Reject return ----

    @Transactional
    public ReturnResponseDto approveReturn(Long returnId, Long brandId) {
        Return return_ = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        // Verify brand owns the order
        if (!return_.getOrder().getItems().get(0).getBrand().getId().equals(brandId)) {
            throw new ResourceNotFoundException("Return not found");
        }

        if (!returnStateMachine.canApprove(return_.getStatus())) {
            throw new IllegalStateException("Return cannot be approved in its current status");
        }

        returnStateMachine.transition(return_, ReturnStatus.APPROVED);
        return_ = returnRepository.save(return_);

        log.info("Return {} approved by brand {}", returnId, brandId);

        // After approving return
        eventPublisher.publishEvent(new ReturnApprovedEvent(this, return_));


        return toResponseDto(return_);
    }

    @Transactional
    public ReturnResponseDto rejectReturn(Long returnId, Long brandId) {
        Return return_ = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        // Verify brand owns the order
        if (!return_.getOrder().getItems().get(0).getBrand().getId().equals(brandId)) {
            throw new ResourceNotFoundException("Return not found");
        }

        if (!returnStateMachine.canReject(return_.getStatus())) {
            throw new IllegalStateException("Return cannot be rejected in its current status");
        }

        returnStateMachine.transition(return_, ReturnStatus.REJECTED);
        return_ = returnRepository.save(return_);

        log.info("Return {} rejected by brand {}", returnId, brandId);

        return toResponseDto(return_);
    }

    // ---- Update delivery status (delivery person) ----

    @Transactional
    public ReturnResponseDto updateReturnStatus(Long returnId, UpdateReturnStatusDto dto) {
        Return return_ = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        returnStateMachine.transition(return_, dto.getStatus());

        // When received, restore inventory
        if (dto.getStatus() == ReturnStatus.RECEIVED) {
            for (ReturnItem item : return_.getItems()) {
                inventoryService.deductStockInternal(
                        item.getOrderItem().getProductVariant().getId(),
                        -item.getQuantityReturned() // negative = add back to stock
                );
            }
            log.info("Inventory restored for return {}", returnId);
        }

        // When completed, initiate Stripe refund
        if (dto.getStatus() == ReturnStatus.COMPLETED) {
            initiateStripeRefund(return_);
        }

        return_ = returnRepository.save(return_);
        return toResponseDto(return_);
    }

    // ---- Stripe refund logic ----

    @Transactional
    public void initiateStripeRefund(Return return_) {
        com.stripe.Stripe.apiKey = stripeSecretKey;

        try {
            var payment = paymentRepository.findByStripePaymentIntentId(
                    return_.getOrder().getId().toString()
            );

            if (payment.isEmpty()) {
                log.warn("Payment not found for order {}, cannot refund", return_.getOrder().getId());
                return;
            }

            long refundAmountPaise = return_.getRefundAmount()
                    .multiply(BigDecimal.valueOf(100)).longValue();

            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(payment.get().getStripePaymentIntentId())
                    .setAmount(refundAmountPaise)
                    .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
                    .build();

            Refund refund = Refund.create(params);
            return_.setStripeRefundId(refund.getId());
            returnStateMachine.transition(return_, ReturnStatus.REFUNDED);
            returnRepository.save(return_);

            log.info("Stripe refund {} initiated for return {}", refund.getId(), return_.getId());

            // Publish event for email notification
            eventPublisher.publishEvent(new ReturnRefundedEvent(this, return_));

        } catch (StripeException e) {
            log.error("Failed to refund return {}: {}", return_.getId(), e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }

    // ---- Fetch returns ----

    public List<ReturnResponseDto> getMyReturns(Long customerId) {
        return returnRepository.findByCustomerId(customerId).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public ReturnResponseDto getReturn(Long returnId, Long customerId) {
        Return return_ = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found"));

        if (!return_.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Return not found");
        }

        return toResponseDto(return_);
    }

    public List<ReturnResponseDto> getPendingReturnsForBrand(Long brandId) {
        // Returns with status REQUESTED or APPROVED for this brand
        List<ReturnStatus> pendingStatuses = List.of(ReturnStatus.REQUESTED, ReturnStatus.APPROVED);

        return returnRepository.findAll().stream()
                .filter(r -> pendingStatuses.contains(r.getStatus()))
                .filter(r -> r.getOrder().getItems().get(0).getBrand().getId().equals(brandId))
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    private ReturnResponseDto toResponseDto(Return return_) {
        List<ReturnItemResponseDto> items = return_.getItems().stream()
                .map(item -> new ReturnItemResponseDto(
                        item.getOrderItem().getId(),
                        item.getOrderItem().getProductNameSnapshot(),
                        item.getQuantityReturned(),
                        item.getRefundPerUnit(),
                        item.getRefundPerUnit().multiply(BigDecimal.valueOf(item.getQuantityReturned()))
                ))
                .collect(Collectors.toList());

        return new ReturnResponseDto(
                return_.getId(),
                return_.getOrder().getId(),
                return_.getStatus(),
                return_.getReason(),
                items,
                return_.getRefundAmount(),
                return_.getCreatedAt(),
                return_.getUpdatedAt()
        );
    }
}
