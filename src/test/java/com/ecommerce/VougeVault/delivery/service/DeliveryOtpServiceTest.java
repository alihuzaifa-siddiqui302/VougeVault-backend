
package com.ecommerce.VougeVault.delivery.service;

import com.ecommerce.VougeVault.delivery.dto.DeliveryLocationDto;
import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.event.DeliveryOtpEvent;
import com.ecommerce.VougeVault.delivery.repository.DeliveryLocationRepository;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.delivery.util.GeolocationUtil;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.shared.exception.BusinessException;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
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
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryOtpServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryTrackingService trackingService;

    @Mock
    private DeliveryLocationRepository locationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private DeliveryOtpService deliveryOtpService;

    private Delivery sampleDelivery;
    private Order sampleOrder;
    private DeliveryLocationDto sampleLocationDto;

    private final Long deliveryId = 100L;
    private final String generatedOtp = "654321";
    private final String redisKey = "delivery:otp:100";

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

        sampleOrder = new Order();
        sampleOrder.setId(500L);
        sampleOrder.setDeliveryLatitude(BigDecimal.valueOf(19.0760));
        sampleOrder.setDeliveryLongitude(BigDecimal.valueOf(72.8777));

        sampleDelivery = new Delivery();
        sampleDelivery.setId(deliveryId);
        sampleDelivery.setOrder(sampleOrder);

        sampleLocationDto = new DeliveryLocationDto(
                null,
                BigDecimal.valueOf(19.0761),
                BigDecimal.valueOf(72.8778),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("generateAndSendOtp() Tests")
    class GenerateAndSendOtpTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery does not exist")
        void generateAndSendOtp_ShouldThrowException_WhenDeliveryNotFound() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryOtpService.generateAndSendOtp(deliveryId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Delivery not found");

            verifyNoInteractions(trackingService, locationRepository, stringRedisTemplate, eventPublisher);
        }

        @Test
        @DisplayName("Should throw BusinessException when location ping is missing in both Redis and DB")
        void generateAndSendOtp_ShouldThrowException_WhenLocationNotFound() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));
            when(trackingService.getLiveLocation(deliveryId)).thenReturn(null);
            when(locationRepository.findLatestByDeliveryId(deliveryId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryOtpService.generateAndSendOtp(deliveryId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("No location ping found for delivery");

            verifyNoInteractions(stringRedisTemplate, eventPublisher);
        }

        @Test
        @DisplayName("Should throw BusinessException when courier has not arrived within arrival threshold")
        void generateAndSendOtp_ShouldThrowException_WhenCourierHasNotArrived() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));
            when(trackingService.getLiveLocation(deliveryId)).thenReturn(sampleLocationDto);

            try (MockedStatic<GeolocationUtil> geoMock = mockStatic(GeolocationUtil.class)) {
                geoMock.when(() -> GeolocationUtil.hasArrivedAtDestination(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                        .thenReturn(false);
                geoMock.when(() -> GeolocationUtil.calculateDistanceKm(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                        .thenReturn(0.85);

                assertThatThrownBy(() -> deliveryOtpService.generateAndSendOtp(deliveryId))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage("You have not arrived at the destination yet. Current distance: 0.85 km");

                verifyNoInteractions(stringRedisTemplate, eventPublisher);
            }
        }

        @Test
        @DisplayName("Should store OTP Hash in Redis with 15-minute TTL and publish DeliveryOtpEvent")
        @SuppressWarnings("unchecked")
        void generateAndSendOtp_ShouldStoreOtpInRedisAndPublishEvent_WhenArrivalValid() {
            when(deliveryRepository.findById(deliveryId)).thenReturn(Optional.of(sampleDelivery));
            when(trackingService.getLiveLocation(deliveryId)).thenReturn(sampleLocationDto);

            try (MockedStatic<GeolocationUtil> geoMock = mockStatic(GeolocationUtil.class)) {
                geoMock.when(() -> GeolocationUtil.hasArrivedAtDestination(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                        .thenReturn(true);
                geoMock.when(GeolocationUtil::generateOtp).thenReturn(generatedOtp);

                deliveryOtpService.generateAndSendOtp(deliveryId);

                ArgumentCaptor<Map<String, String>> mapCaptor = ArgumentCaptor.forClass(Map.class);
                verify(hashOperations).putAll(eq(redisKey), mapCaptor.capture());

                Map<String, String> savedFields = mapCaptor.getValue();
                assertThat(savedFields.get("code")).isEqualTo(generatedOtp);
                assertThat(savedFields.get("attempts")).isEqualTo("3");

                verify(stringRedisTemplate).expire(redisKey, Duration.ofMinutes(15));
                verify(eventPublisher).publishEvent(any(DeliveryOtpEvent.class));
            }
        }
    }

    @Nested
    @DisplayName("verifyOtp() Tests")
    class VerifyOtpTests {

        @Test
        @DisplayName("Should throw BusinessException when OTP has expired or was not requested")
        void verifyOtp_ShouldThrowException_WhenOtpExpiredOrMissing() {
            when(hashOperations.get(redisKey, "code")).thenReturn(null);
            when(hashOperations.get(redisKey, "attempts")).thenReturn(null);

            assertThatThrownBy(() -> deliveryOtpService.verifyOtp(deliveryId, "123456", deliveryService))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("OTP has expired or was not requested. Please ask for a new code.");

            verifyNoInteractions(deliveryService);
        }

        @Test
        @DisplayName("Should delete key and throw BusinessException when attempts left are zero or below")
        void verifyOtp_ShouldDeleteKeyAndThrow_WhenNoAttemptsLeft() {
            when(hashOperations.get(redisKey, "code")).thenReturn("123456");
            when(hashOperations.get(redisKey, "attempts")).thenReturn("0");

            assertThatThrownBy(() -> deliveryOtpService.verifyOtp(deliveryId, "123456", deliveryService))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Too many incorrect attempts. Please generate a new OTP.");

            verify(stringRedisTemplate).delete(redisKey);
            verifyNoInteractions(deliveryService);
        }

        @Test
        @DisplayName("Should decrement attempts and throw BusinessException when entered OTP is incorrect")
        void verifyOtp_ShouldDecrementAttemptsAndThrow_WhenOtpIncorrect() {
            when(hashOperations.get(redisKey, "code")).thenReturn("123456");
            when(hashOperations.get(redisKey, "attempts")).thenReturn("3");
            when(hashOperations.increment(redisKey, "attempts", -1)).thenReturn(2L);

            assertThatThrownBy(() -> deliveryOtpService.verifyOtp(deliveryId, "999999", deliveryService))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Incorrect OTP. 2 attempts remaining.");

            verify(hashOperations).increment(redisKey, "attempts", -1);
            verifyNoInteractions(deliveryService);
        }

        @Test
        @DisplayName("Should delete key and throw when last incorrect attempt is exhausted")
        void verifyOtp_ShouldDeleteKeyAndThrow_WhenLastAttemptExhausted() {
            when(hashOperations.get(redisKey, "code")).thenReturn("123456");
            when(hashOperations.get(redisKey, "attempts")).thenReturn("1");
            when(hashOperations.increment(redisKey, "attempts", -1)).thenReturn(0L);

            assertThatThrownBy(() -> deliveryOtpService.verifyOtp(deliveryId, "999999", deliveryService))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Incorrect OTP. No attempts left. Please request a new code.");

            verify(stringRedisTemplate).delete(redisKey);
            verifyNoInteractions(deliveryService);
        }

        @Test
        @DisplayName("Should delete Redis key and trigger confirmDeliveryByOtp when entered OTP is correct")
        void verifyOtp_ShouldCompleteVerification_WhenOtpMatches() {
            when(hashOperations.get(redisKey, "code")).thenReturn("123456");
            when(hashOperations.get(redisKey, "attempts")).thenReturn("3");

            deliveryOtpService.verifyOtp(deliveryId, "123456", deliveryService);

            verify(stringRedisTemplate).delete(redisKey);
            verify(deliveryService).confirmDeliveryByOtp(deliveryId);
        }
    }
}