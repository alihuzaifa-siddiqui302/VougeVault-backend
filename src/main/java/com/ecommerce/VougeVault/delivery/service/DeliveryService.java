package com.ecommerce.VougeVault.delivery.service;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.delivery.dto.DeliveryResponseDto;
import com.ecommerce.VougeVault.delivery.dto.UpdateDeliveryStatusDto;
import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import com.ecommerce.VougeVault.delivery.event.DeliveryStatusChangedEvent;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;
    private final DeliveryStatsService statsService;
    private final DeliveryTrackingService trackingService;
    private final DeliveryOtpService otpService;
    private final ApplicationEventPublisher eventPublisher;


    public List<DeliveryResponseDto> getMyDeliveries(Long deliveryPersonId) {
        return deliveryRepository.findByDeliveryPersonIdAndStatusNot(
                        deliveryPersonId,
                        DeliveryStatus.DELIVERED
                ).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public DeliveryResponseDto getDelivery(Long deliveryId, Long deliveryPersonId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        if (delivery.getDeliveryPerson() == null ||
                !delivery.getDeliveryPerson().getUser().getId().equals(deliveryPersonId)) {
            throw new ResourceNotFoundException("Delivery not found");
        }

        return toResponseDto(delivery);
    }

    @Transactional
    public DeliveryResponseDto updateDeliveryStatus(Long deliveryId, Long deliveryPersonId, UpdateDeliveryStatusDto dto) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        if (delivery.getDeliveryPerson() == null ||
                !delivery.getDeliveryPerson().getUser().getId().equals(deliveryPersonId)) {
            throw new ResourceNotFoundException("Delivery not found");
        }

        // Validate status transition
        if (!isValidTransition(delivery.getStatus(), dto.getStatus())) {
            throw new IllegalStateException(
                    "Cannot transition from " + delivery.getStatus() + " to " + dto.getStatus()
            );
        }

        // OLD status
        DeliveryStatus oldStatus = delivery.getStatus();

        // UPDATE status
        delivery.setStatus(dto.getStatus());
        deliveryRepository.save(delivery);

        // TRIGGER appropriate actions based on NEW status
        if (dto.getStatus() == DeliveryStatus.OUT_FOR_DELIVERY) {
            onOutForDelivery(delivery);
        } else if (dto.getStatus() == DeliveryStatus.DELIVERED) {
            onDeliveryCompleted(delivery);
        } else if (dto.getStatus() == DeliveryStatus.FAILED) {
            onDeliveryFailed(delivery);
        }

        // Publish event for emails
        eventPublisher.publishEvent(new DeliveryStatusChangedEvent(this, delivery, dto.getStatus()));

        log.info("Delivery {} status changed from {} to {}",
                deliveryId, oldStatus, dto.getStatus());

        return toResponseDto(delivery);
    }


    @Transactional
    private void onOutForDelivery(Delivery delivery) {
        log.info("Delivery {} marked as OUT_FOR_DELIVERY", delivery.getId());

        // Update order status
        Order order = delivery.getOrder();
        order.setStatus(com.ecommerce.VougeVault.order.entity.OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Notify customer: "Delivery person is on the way"
        log.info("Customer notified: Delivery person on the way for order {}", order.getId());
    }


    @Transactional
    public void onDeliveryCompleted(Delivery delivery) {
        log.info("Delivery {} marked as DELIVERED", delivery.getId());

        // Update order status
        Order order = delivery.getOrder();
        order.setStatus(com.ecommerce.VougeVault.order.entity.OrderStatus.DELIVERED);
        orderRepository.save(order);

        // Record successful delivery + add earnings
        if (delivery.getDeliveryPerson() != null) {
            statsService.recordSuccessfulDelivery(
                    delivery.getDeliveryPerson().getId(),
                    BigDecimal.valueOf(100)  // Base delivery amount (₹100)
            );
            log.info("Stats updated for delivery person: +1 successful delivery");
        }

        // Notify customer: "Delivery confirmed! You can now leave a review"
        log.info("Customer notified: Order {} delivered successfully", order.getId());
    }

    /**
     * LIFECYCLE HOOK: When delivery status changes to FAILED
     *
     * Triggered by: Delivery person marks delivery as failed
     * Records failed delivery
     * Updates delivery person stats
     */
    @Transactional
    private void onDeliveryFailed(Delivery delivery) {
        log.info("Delivery {} marked as FAILED", delivery.getId());

        // Record failed delivery
        if (delivery.getDeliveryPerson() != null) {
            statsService.recordFailedDelivery(delivery.getDeliveryPerson().getId());
            log.info("Stats updated for delivery person: +1 failed delivery");
        }

        // Update order status
        Order order = delivery.getOrder();
        order.setStatus(com.ecommerce.VougeVault.order.entity.OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Notify customer: "Delivery failed, we'll retry tomorrow"
        log.info("Customer notified: Delivery {} failed", delivery.getId());
    }

    /**
     * LIFECYCLE HOOK: When OTP is verified
     *
     * This is called from DeliveryOtpService.verifyOtp()
     * After OTP verification, mark delivery as completed
     */
    @Transactional
    public void confirmDeliveryByOtp(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        // Mark as DELIVERED
        delivery.setStatus(DeliveryStatus.DELIVERED);
        deliveryRepository.save(delivery);

        // Evict live GPS cache immediately
        trackingService.clearTrackingSession(deliveryId);

        // Trigger lifecycle hook
        onDeliveryCompleted(delivery);

        log.info("Delivery {} confirmed via OTP verification", deliveryId);
    }

    /**
     * Check if status transition is valid
     *
     * Valid transitions:
     * ASSIGNED → PICKED_UP → OUT_FOR_DELIVERY → DELIVERED
     *                      → FAILED
     */
    private boolean isValidTransition(DeliveryStatus from, DeliveryStatus to) {
        return switch (from) {
            case ASSIGNED -> to == DeliveryStatus.PICKED_UP;
            case PICKED_UP -> to == DeliveryStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> to == DeliveryStatus.DELIVERED || to == DeliveryStatus.FAILED;
            case DELIVERED, FAILED -> false;  // No transitions from terminal states
        };
    }

    private DeliveryResponseDto toResponseDto(Delivery delivery) {
        String deliveryPersonName = "Unassigned";
        String deliveryPersonPhone = null;

        if (delivery.getDeliveryPerson() != null && delivery.getDeliveryPerson().getUser() != null) {
            deliveryPersonName = delivery.getDeliveryPerson().getUser().getName();
        }

        return new DeliveryResponseDto(
                delivery.getId(),
                delivery.getOrder() != null ? delivery.getOrder().getId() : null,
                deliveryPersonName,
                delivery.getStatus(),
                delivery.getPickupAddress(),
                delivery.getDropAddress(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}