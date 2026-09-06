package com.ecommerce.VougeVault.delivery.service;

import com.ecommerce.VougeVault.delivery.dto.DeliveryLocationDto;
import com.ecommerce.VougeVault.delivery.dto.DeliveryTrackingDto;
import com.ecommerce.VougeVault.delivery.dto.UpdateLocationDto;
import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.delivery.util.GeolocationUtil;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.ecommerce.VougeVault.user.entity.User;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryTrackingServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private DeliveryTrackingService deliveryTrackingService;

    private User sampleCustomerUser;
    private User sampleDeliveryUser;
    private DeliveryPerson sampleDeliveryPerson;
    private Order sampleOrder;
    private Delivery sampleDelivery;
    private DeliveryLocationDto sampleLocationDto;
    private UpdateLocationDto updateLocationDto;

    private final Long deliveryId = 100L;
    private final Long orderId = 500L;
    private final Long customerId = 1L;
    private final Long deliveryPersonUserId = 10L;
    private final String expectedRedisKey = "delivery:location:100";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        sampleCustomerUser = new User();
        sampleCustomerUser.setId(customerId);
        sampleCustomerUser.setName("Jane Customer");

        sampleDeliveryUser = new User();
        sampleDeliveryUser.setId(deliveryPersonUserId);
        sampleDeliveryUser.setName("Alex Courier");

        sampleDeliveryPerson = new DeliveryPerson();
        sampleDeliveryPerson.setId(20L);
        sampleDeliveryPerson.setUser(sampleDeliveryUser);

        sampleOrder = new Order();
        sampleOrder.setId(orderId);
        sampleOrder.setCustomer(sampleCustomerUser);
        sampleOrder.setDeliveryAddress("Flat 401, Grand Residency");
        sampleOrder.setDeliveryCity("Mumbai");
        sampleOrder.setDeliveryLatitude(new BigDecimal("19.0760"));
        sampleOrder.setDeliveryLongitude(new BigDecimal("72.8777"));

        sampleDelivery = new Delivery();
        sampleDelivery.setId(deliveryId);
        sampleDelivery.setOrder(sampleOrder);
        sampleDelivery.setDeliveryPerson(sampleDeliveryPerson);
        sampleDelivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);

        sampleLocationDto = new DeliveryLocationDto(
                null,
                new BigDecimal("19.0720"),
                new BigDecimal("72.8750"),
                LocalDateTime.now()
        );

        updateLocationDto = new UpdateLocationDto();
        updateLocationDto.setLatitude(new BigDecimal("19.0720"));
        updateLocationDto.setLongitude(new BigDecimal("72.8750"));
    }

    @Nested
    @DisplayName("updateLocation() Tests")
    class UpdateLocationTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery does not exist")
        void updateLocation_ShouldThrowException_WhenDeliveryNotFound() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryTrackingService.updateLocation(deliveryId, deliveryPersonUserId, updateLocationDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageStartingWith("Delivery not found");

            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery has no delivery person assigned")
        void updateLocation_ShouldThrowException_WhenDeliveryPersonIsNull() {
            sampleDelivery.setDeliveryPerson(null);
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            assertThatThrownBy(() -> deliveryTrackingService.updateLocation(deliveryId, deliveryPersonUserId, updateLocationDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageStartingWith("Delivery not found");

            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery person user ID mismatches")
        void updateLocation_ShouldThrowException_WhenDeliveryPersonUserIdMismatch() {
            Long unauthorizedUserId = 999L;
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            assertThatThrownBy(() -> deliveryTrackingService.updateLocation(deliveryId, unauthorizedUserId, updateLocationDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageStartingWith("Delivery not found");

            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("Should write location directly to Redis with 2-hour TTL when authorized")
        void updateLocation_ShouldCacheInRedisAndReturnDto_WhenValid() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));

            DeliveryLocationDto result = deliveryTrackingService.updateLocation(deliveryId, deliveryPersonUserId, updateLocationDto);

            ArgumentCaptor<DeliveryLocationDto> locationCaptor = ArgumentCaptor.forClass(DeliveryLocationDto.class);
            verify(valueOperations).set(eq(expectedRedisKey), locationCaptor.capture(), eq(2L), eq(TimeUnit.HOURS));

            DeliveryLocationDto cachedLocation = locationCaptor.getValue();
            assertThat(cachedLocation.getLocationId()).isNull();
            assertThat(cachedLocation.getLatitude()).isEqualByComparingTo("19.0720");
            assertThat(cachedLocation.getLongitude()).isEqualByComparingTo("72.8750");
            assertThat(cachedLocation.getCreatedAt()).isNotNull();

            assertThat(result).isNotNull();
            assertThat(result.getLocationId()).isNull();
            assertThat(result.getLatitude()).isEqualByComparingTo("19.0720");
            assertThat(result.getLongitude()).isEqualByComparingTo("72.8750");
        }
    }

    @Nested
    @DisplayName("trackDelivery() Tests")
    class TrackDeliveryTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery does not exist for order ID")
        void trackDelivery_ShouldThrowException_WhenDeliveryNotFound() {
            when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryTrackingService.trackDelivery(orderId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");

            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when requesting customer does not own order")
        void trackDelivery_ShouldThrowException_WhenCustomerIdMismatch() {
            Long unauthorizedCustomerId = 888L;
            when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(sampleDelivery));

            assertThatThrownBy(() -> deliveryTrackingService.trackDelivery(orderId, unauthorizedCustomerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");

            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("Should return DeliveryTrackingDto with null metrics when no location is in Redis")
        void trackDelivery_ShouldReturnDtoWithNullMetrics_WhenNoLocationExists() {
            when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(sampleDelivery));
            when(valueOperations.get(expectedRedisKey)).thenReturn(null);

            DeliveryTrackingDto result = deliveryTrackingService.trackDelivery(orderId, customerId);

            assertThat(result).isNotNull();
            assertThat(result.getDeliveryId()).isEqualTo(deliveryId);
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getDeliveryPersonName()).isEqualTo("Alex Courier");
            assertThat(result.getDeliveryStatus()).isEqualTo("OUT_FOR_DELIVERY");
            assertThat(result.getCurrentLocation()).isNull();
            assertThat(result.getDistanceToDestinationKm()).isNull();
            assertThat(result.getEstimatedMinutesToArrive()).isNull();
            assertThat(result.getEstimatedDeliveryTime()).isNull();
        }

        @Test
        @DisplayName("Should calculate distance, ETA, and return populated tracking DTO when location exists in Redis")
        void trackDelivery_ShouldCalculateMetricsAndReturnDto_WhenLocationExists() {
            when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(sampleDelivery));
            when(valueOperations.get(expectedRedisKey)).thenReturn(sampleLocationDto);

            try (MockedStatic<GeolocationUtil> geoMock = mockStatic(GeolocationUtil.class)) {
                geoMock.when(() -> GeolocationUtil.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                        .thenReturn(3.5);
                geoMock.when(() -> GeolocationUtil.estimateTimeMinutes(3.5))
                        .thenReturn(15);

                DeliveryTrackingDto result = deliveryTrackingService.trackDelivery(orderId, customerId);

                assertThat(result).isNotNull();
                assertThat(result.getDeliveryId()).isEqualTo(deliveryId);
                assertThat(result.getOrderId()).isEqualTo(orderId);
                assertThat(result.getDeliveryPersonName()).isEqualTo("Alex Courier");
                assertThat(result.getDeliveryStatus()).isEqualTo("OUT_FOR_DELIVERY");
                assertThat(result.getCurrentLocation()).isNotNull();
                assertThat(result.getCurrentLocation().getLatitude()).isEqualByComparingTo("19.0720");
                assertThat(result.getDistanceToDestinationKm()).isEqualTo(3.5);
                assertThat(result.getEstimatedMinutesToArrive()).isEqualTo(15);
                assertThat(result.getEstimatedDeliveryTime()).startsWith("Today by ");
            }
        }

        @Test
        @DisplayName("Should fallback delivery person name to 'Unassigned' when deliveryPerson is null")
        void trackDelivery_ShouldFallbackToUnassigned_WhenDeliveryPersonNull() {
            sampleDelivery.setDeliveryPerson(null);
            when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(sampleDelivery));
            when(valueOperations.get(expectedRedisKey)).thenReturn(null);

            DeliveryTrackingDto result = deliveryTrackingService.trackDelivery(orderId, customerId);

            assertThat(result.getDeliveryPersonName()).isEqualTo("Unassigned");
        }
    }

    @Nested
    @DisplayName("clearTrackingSession() Tests")
    class ClearTrackingSessionTests {

        @Test
        @DisplayName("Should delete key from Redis when clearing tracking session")
        void clearTrackingSession_ShouldDeleteRedisKey() {
            deliveryTrackingService.clearTrackingSession(deliveryId);

            verify(redisTemplate, times(1)).delete(expectedRedisKey);
        }
    }
}