package com.ecommerce.VougeVault.delivery.service;

import com.ecommerce.VougeVault.delivery.dto.DeliveryResponseDto;
import com.ecommerce.VougeVault.delivery.dto.UpdateDeliveryStatusDto;
import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import com.ecommerce.VougeVault.delivery.event.DeliveryStatusChangedEvent;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.ecommerce.VougeVault.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeliveryStatsService statsService;

    @Mock
    private DeliveryOtpService otpService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DeliveryTrackingService trackingService;

    @InjectMocks
    private DeliveryService deliveryService;

    private User sampleUser;
    private DeliveryPerson sampleDeliveryPerson;
    private Order sampleOrder;
    private Delivery sampleDelivery;
    private UpdateDeliveryStatusDto updateStatusDto;

    private final Long deliveryPersonUserId = 1L;
    private final Long deliveryPersonId = 10L;
    private final Long deliveryId = 100L;
    private final Long orderId = 500L;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(deliveryPersonUserId);
        sampleUser.setName("John Courier");

        sampleDeliveryPerson = new DeliveryPerson();
        sampleDeliveryPerson.setId(deliveryPersonId);
        sampleDeliveryPerson.setUser(sampleUser);

        sampleOrder = new Order();
        sampleOrder.setId(orderId);
        sampleOrder.setStatus(OrderStatus.PAID);

        sampleDelivery = new Delivery();
        sampleDelivery.setId(deliveryId);
        sampleDelivery.setOrder(sampleOrder);
        sampleDelivery.setDeliveryPerson(sampleDeliveryPerson);
        sampleDelivery.setStatus(DeliveryStatus.ASSIGNED);
        sampleDelivery.setPickupAddress("Warehouse 4B, Industrial Zone");
        sampleDelivery.setDropAddress("Flat 202, Skyline Towers");
        sampleDelivery.setCreatedAt(LocalDateTime.now());
        sampleDelivery.setUpdatedAt(LocalDateTime.now());

        updateStatusDto = new UpdateDeliveryStatusDto();
    }

    @Nested
    @DisplayName("getMyDeliveries() Tests")
    class GetMyDeliveriesTests {

        @Test
        @DisplayName("Should return active deliveries excluding DELIVERED status")
        void getMyDeliveries_ShouldReturnActiveDeliveries() {
            when(deliveryRepository.findByDeliveryPersonIdAndStatusNot(deliveryPersonUserId, DeliveryStatus.DELIVERED))
                    .thenReturn(List.of(sampleDelivery));

            List<DeliveryResponseDto> result = deliveryService.getMyDeliveries(deliveryPersonUserId);

            assertThat(result).hasSize(1);
            DeliveryResponseDto dto = result.get(0);
            assertThat(dto.getDeliveryId()).isEqualTo(deliveryId);
            assertThat(dto.getOrderId()).isEqualTo(orderId);
            assertThat(dto.getDeliveryPersonName()).isEqualTo("John Courier");
            assertThat(dto.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
            assertThat(dto.getPickupAddress()).isEqualTo("Warehouse 4B, Industrial Zone");
            assertThat(dto.getDropAddress()).isEqualTo("Flat 202, Skyline Towers");
        }

        @Test
        @DisplayName("Should return empty list when no active deliveries exist")
        void getMyDeliveries_ShouldReturnEmptyList_WhenNoneFound() {
            when(deliveryRepository.findByDeliveryPersonIdAndStatusNot(deliveryPersonUserId, DeliveryStatus.DELIVERED))
                    .thenReturn(Collections.emptyList());

            List<DeliveryResponseDto> result = deliveryService.getMyDeliveries(deliveryPersonUserId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDelivery() Tests")
    class GetDeliveryTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery does not exist")
        void getDelivery_ShouldThrowException_WhenDeliveryNotFound() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, deliveryPersonUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery has no delivery person assigned")
        void getDelivery_ShouldThrowException_WhenDeliveryPersonIsNull() {
            sampleDelivery.setDeliveryPerson(null);
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, deliveryPersonUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery person user ID mismatches")
        void getDelivery_ShouldThrowException_WhenDeliveryPersonUserMismatch() {
            Long unauthorizedUserId = 999L;
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            assertThatThrownBy(() -> deliveryService.getDelivery(deliveryId, unauthorizedUserId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");
        }

        @Test
        @DisplayName("Should return mapped DeliveryResponseDto when delivery exists and user matches")
        void getDelivery_ShouldReturnDto_WhenFoundAndAuthorized() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            DeliveryResponseDto result = deliveryService.getDelivery(deliveryId, deliveryPersonUserId);

            assertThat(result).isNotNull();
            assertThat(result.getDeliveryId()).isEqualTo(deliveryId);
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getDeliveryPersonName()).isEqualTo("John Courier");
            assertThat(result.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
        }
    }

    @Nested
    @DisplayName("updateDeliveryStatus() Transition & Action Tests")
    class UpdateDeliveryStatusTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery does not exist")
        void updateDeliveryStatus_ShouldThrowException_WhenDeliveryNotFound() {
            updateStatusDto.setStatus(DeliveryStatus.PICKED_UP);
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.updateDeliveryStatus(deliveryId, deliveryPersonUserId, updateStatusDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");

            verifyNoInteractions(eventPublisher, statsService);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when caller is not the assigned delivery person")
        void updateDeliveryStatus_ShouldThrowException_WhenUnauthorizedUser() {
            updateStatusDto.setStatus(DeliveryStatus.PICKED_UP);
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            assertThatThrownBy(() -> deliveryService.updateDeliveryStatus(deliveryId, 999L, updateStatusDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");

            verify(deliveryRepository, never()).save(any(Delivery.class));
            verifyNoInteractions(eventPublisher);
        }

        @ParameterizedTest(name = "Invalid transition from {0} to {1}")
        @CsvSource({
                "ASSIGNED, OUT_FOR_DELIVERY",
                "ASSIGNED, DELIVERED",
                "ASSIGNED, FAILED",
                "PICKED_UP, DELIVERED",
                "PICKED_UP, FAILED",
                "DELIVERED, PICKED_UP",
                "FAILED, OUT_FOR_DELIVERY"
        })
        @DisplayName("Should throw IllegalStateException on illegal status transitions")
        void updateDeliveryStatus_ShouldThrowException_OnInvalidTransitions(DeliveryStatus from, DeliveryStatus to) {
            sampleDelivery.setStatus(from);
            updateStatusDto.setStatus(to);

            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            assertThatThrownBy(() -> deliveryService.updateDeliveryStatus(deliveryId, deliveryPersonUserId, updateStatusDto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Cannot transition from " + from + " to " + to);

            verify(deliveryRepository, never()).save(any(Delivery.class));
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Transition ASSIGNED -> PICKED_UP: Should update delivery and publish event without order changes")
        void updateDeliveryStatus_ShouldTransitionToPickedUp() {
            sampleDelivery.setStatus(DeliveryStatus.ASSIGNED);
            updateStatusDto.setStatus(DeliveryStatus.PICKED_UP);

            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DeliveryResponseDto response = deliveryService.updateDeliveryStatus(deliveryId, deliveryPersonUserId, updateStatusDto);

            assertThat(sampleDelivery.getStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
            verify(deliveryRepository).save(sampleDelivery);
            verify(eventPublisher).publishEvent(any(DeliveryStatusChangedEvent.class));
            verifyNoInteractions(orderRepository, statsService);
            assertThat(response.getStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
        }

        @Test
        @DisplayName("Transition PICKED_UP -> OUT_FOR_DELIVERY: Should update order to CONFIRMED and publish event")
        void updateDeliveryStatus_ShouldTriggerOnOutForDelivery() {
            sampleDelivery.setStatus(DeliveryStatus.PICKED_UP);
            updateStatusDto.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);

            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DeliveryResponseDto response = deliveryService.updateDeliveryStatus(deliveryId, deliveryPersonUserId, updateStatusDto);

            assertThat(sampleDelivery.getStatus()).isEqualTo(DeliveryStatus.OUT_FOR_DELIVERY);
            assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            verify(orderRepository).save(sampleOrder);
            verify(eventPublisher).publishEvent(any(DeliveryStatusChangedEvent.class));
            assertThat(response.getStatus()).isEqualTo(DeliveryStatus.OUT_FOR_DELIVERY);
        }

        @Test
        @DisplayName("Transition OUT_FOR_DELIVERY -> DELIVERED: Should update order to DELIVERED, credit stats, and publish event")
        void updateDeliveryStatus_ShouldTriggerOnDeliveryCompleted() {
            sampleDelivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
            updateStatusDto.setStatus(DeliveryStatus.DELIVERED);

            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DeliveryResponseDto response = deliveryService.updateDeliveryStatus(deliveryId, deliveryPersonUserId, updateStatusDto);

            assertThat(sampleDelivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            verify(orderRepository).save(sampleOrder);
            verify(statsService).recordSuccessfulDelivery(deliveryPersonId, BigDecimal.valueOf(100));
            verify(eventPublisher).publishEvent(any(DeliveryStatusChangedEvent.class));
            assertThat(response.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
        }

        @Test
        @DisplayName("Transition OUT_FOR_DELIVERY -> FAILED: Should update order to CANCELLED, record failed stats, and publish event")
        void updateDeliveryStatus_ShouldTriggerOnDeliveryFailed() {
            sampleDelivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
            updateStatusDto.setStatus(DeliveryStatus.FAILED);

            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));
            when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

            DeliveryResponseDto response = deliveryService.updateDeliveryStatus(deliveryId, deliveryPersonUserId, updateStatusDto);

            assertThat(sampleDelivery.getStatus()).isEqualTo(DeliveryStatus.FAILED);
            assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(sampleOrder);
            verify(statsService).recordFailedDelivery(deliveryPersonId);
            verify(eventPublisher).publishEvent(any(DeliveryStatusChangedEvent.class));
            assertThat(response.getStatus()).isEqualTo(DeliveryStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("confirmDeliveryByOtp() Tests")
    class ConfirmDeliveryByOtpTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery does not exist")
        void confirmDeliveryByOtp_ShouldThrowException_WhenDeliveryNotFound() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryService.confirmDeliveryByOtp(deliveryId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");

            verify(deliveryRepository, never()).save(any(Delivery.class));
            verifyNoInteractions(orderRepository, statsService);
        }

        @Test
        @DisplayName("Should mark delivery DELIVERED, update order status, and credit delivery stats")
        void confirmDeliveryByOtp_ShouldCompleteDeliverySuccessfully() {
            sampleDelivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            deliveryService.confirmDeliveryByOtp(deliveryId);

            assertThat(sampleDelivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            verify(deliveryRepository).save(sampleDelivery);
            assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            verify(orderRepository).save(sampleOrder);
            verify(statsService).recordSuccessfulDelivery(deliveryPersonId, BigDecimal.valueOf(100));

            // Verify Redis tracking key is evicted
            verify(trackingService).clearTrackingSession(deliveryId);
        }

        @Test
        @DisplayName("Should update order status without throwing when deliveryPerson is null during completion")
        void confirmDeliveryByOtp_ShouldHandleNullDeliveryPersonGracefully() {
            sampleDelivery.setDeliveryPerson(null);
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            deliveryService.confirmDeliveryByOtp(deliveryId);

            assertThat(sampleDelivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
            verify(orderRepository).save(sampleOrder);
            verifyNoInteractions(statsService);

            // Verify Redis tracking key is evicted
            verify(trackingService).clearTrackingSession(deliveryId);
        }
    }

    @Nested
    @DisplayName("toResponseDto() Edge Cases")
    class DtoMappingTests {

        @Test
        @DisplayName("Should fallback delivery person name to 'Unassigned' when deliveryPerson is null")
        void toResponseDto_ShouldFallbackToUnassigned_WhenDeliveryPersonNull() {
            sampleDelivery.setDeliveryPerson(null);
            when(deliveryRepository.findByDeliveryPersonIdAndStatusNot(deliveryPersonUserId, DeliveryStatus.DELIVERED))
                    .thenReturn(List.of(sampleDelivery));

            List<DeliveryResponseDto> result = deliveryService.getMyDeliveries(deliveryPersonUserId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeliveryPersonName()).isEqualTo("Unassigned");
        }
    }
}