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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryOtpService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryTrackingService trackingService;
    private final DeliveryLocationRepository locationRepository; // Fallback if Redis cache is empty
    private final ApplicationEventPublisher eventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String OTP_KEY_PREFIX = "delivery:otp:";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_ATTEMPTS = "attempts";
    private static final Duration OTP_EXPIRATION = Duration.ofMinutes(15);
    private static final int MAX_ATTEMPTS = 3;

    @Transactional
    public void generateAndSendOtp(Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        // 1. Retrieve latest location from Redis cache; fallback to DB if absent
        double agentLat;
        double agentLon;

        DeliveryLocationDto cachedLocation = trackingService.getLiveLocation(deliveryId);
        if (cachedLocation != null) {
            agentLat = cachedLocation.getLatitude().doubleValue();
            agentLon = cachedLocation.getLongitude().doubleValue();
        } else {
            var dbLocation = locationRepository.findLatestByDeliveryId(deliveryId)
                    .orElseThrow(() -> new BusinessException("No location ping found for delivery"));
            agentLat = (dbLocation.getLatitude().doubleValue());
            agentLon = (dbLocation.getLongitude().doubleValue());
        }

        // 2. Safe destination fallback (avoiding NPE on unboxing)
        Order order = delivery.getOrder();
        double destLat = (order.getDeliveryLatitude() != null) ? order.getDeliveryLatitude().doubleValue() : 19.0760;
        double destLon = (order.getDeliveryLongitude() != null) ? order.getDeliveryLongitude().doubleValue() : 72.8777;

        // 3. Proximity check (must be within 100 meters)
        boolean hasArrived = GeolocationUtil.hasArrivedAtDestination(agentLat, agentLon, destLat, destLon);
        if (!hasArrived) {
            double distanceKm = GeolocationUtil.calculateDistanceKm(agentLat, agentLon, destLat, destLon);
            throw new BusinessException(String.format("You have not arrived at the destination yet. Current distance: %.2f km", distanceKm));
        }

        // 4. Generate 6-digit OTP and store in Redis Hash with 15-minute TTL
        String otpCode = GeolocationUtil.generateOtp();
        String redisKey = OTP_KEY_PREFIX + deliveryId;

        stringRedisTemplate.opsForHash().putAll(redisKey, Map.of(
                FIELD_CODE, otpCode,
                FIELD_ATTEMPTS, String.valueOf(MAX_ATTEMPTS)
        ));
        stringRedisTemplate.expire(redisKey, OTP_EXPIRATION);

        // 5. Notify customer via event listener
        eventPublisher.publishEvent(new DeliveryOtpEvent(this, delivery, otpCode));
        log.info("15-minute OTP generated in Redis for delivery #{}", deliveryId);
    }

    @Transactional
    public void verifyOtp(Long deliveryId, String enteredOtp, DeliveryService deliveryService) {
        String redisKey = OTP_KEY_PREFIX + deliveryId;

        // 1. Fetch OTP session from Redis
        Object codeObj = stringRedisTemplate.opsForHash().get(redisKey, FIELD_CODE);
        Object attemptsObj = stringRedisTemplate.opsForHash().get(redisKey, FIELD_ATTEMPTS);

        if (codeObj == null || attemptsObj == null) {
            throw new BusinessException("OTP has expired or was not requested. Please ask for a new code.");
        }

        String actualOtp = codeObj.toString();
        int attemptsLeft = Integer.parseInt(attemptsObj.toString());

        if (attemptsLeft <= 0) {
            stringRedisTemplate.delete(redisKey);
            throw new BusinessException("Too many incorrect attempts. Please generate a new OTP.");
        }

        // 2. Verify submitted code
        if (!actualOtp.equals(enteredOtp.trim())) {
            long remaining = stringRedisTemplate.opsForHash().increment(redisKey, FIELD_ATTEMPTS, -1);
            if (remaining <= 0) {
                stringRedisTemplate.delete(redisKey);
                throw new BusinessException("Incorrect OTP. No attempts left. Please request a new code.");
            }
            throw new BusinessException("Incorrect OTP. " + remaining + " attempts remaining.");
        }

        // 3. Cleanup Redis key on success and update persistent status
        stringRedisTemplate.delete(redisKey);
        deliveryService.confirmDeliveryByOtp(deliveryId);

        log.info("OTP verified successfully for delivery #{}. Cache cleared.", deliveryId);
    }
}