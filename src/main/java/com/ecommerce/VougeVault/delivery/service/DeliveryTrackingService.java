package com.ecommerce.VougeVault.delivery.service;

import com.ecommerce.VougeVault.delivery.dto.DeliveryLocationDto;
import com.ecommerce.VougeVault.delivery.dto.DeliveryTrackingDto;
import com.ecommerce.VougeVault.delivery.dto.UpdateLocationDto;
import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.delivery.util.GeolocationUtil;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryTrackingService {

    private final DeliveryRepository deliveryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOCATION_KEY_PREFIX = "delivery:location:";
    private static final long LOCATION_TTL_HOURS = 2;

    /**
     * Pure Redis Ping:
     * 1. Performs a fast read-only check on the delivery and assigned driver.
     * 2. Writes coordinates directly to Redis memory (TTL 2 hours).
     * 3. Zero writes to PostgreSQL.
     */
    @Transactional(readOnly = true)
    public DeliveryLocationDto updateLocation(Long deliveryId, Long deliveryPersonId, UpdateLocationDto dto) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        if (delivery.getDeliveryPerson() == null ||
                !delivery.getDeliveryPerson().getUser().getId().equals(deliveryPersonId)) {
            throw new ResourceNotFoundException("Delivery not found or unauthorized");
        }

        DeliveryLocationDto locationDto = new DeliveryLocationDto(
                null, // No DB primary key needed for ephemeral in-memory pings
                dto.getLatitude(),
                dto.getLongitude(),
                LocalDateTime.now()
        );

        // Store directly in Redis
        String redisKey = LOCATION_KEY_PREFIX + deliveryId;
        redisTemplate.opsForValue().set(redisKey, locationDto, LOCATION_TTL_HOURS, TimeUnit.HOURS);

        log.debug("Live location cached in Redis for delivery #{}: [{}, {}]",
                deliveryId, dto.getLatitude(), dto.getLongitude());

        return locationDto;
    }

    /**
     * Direct Redis lookup for live coordinates (used by customer tracking and DeliveryOtpService).
     */
    public DeliveryLocationDto getLiveLocation(Long deliveryId) {
        String redisKey = LOCATION_KEY_PREFIX + deliveryId;
        Object data = redisTemplate.opsForValue().get(redisKey);

        if (data instanceof DeliveryLocationDto locationDto) {
            return locationDto;
        }

        return null;
    }

    /**
     * Customer tracking endpoint:
     * Reads current coordinates from Redis and computes ETA and distance.
     */
    @Transactional(readOnly = true)
    public DeliveryTrackingDto trackDelivery(Long orderId, Long customerId) {
        Delivery delivery = deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));

        if (!delivery.getOrder().getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Delivery not found");
        }

        // Fetch coordinates purely from Redis
        DeliveryLocationDto currentLocation = getLiveLocation(delivery.getId());

        Double distanceKm = null;
        Integer estimatedMinutes = null;
        String estimatedDeliveryTime = null;

        if (currentLocation != null) {
            Order order = delivery.getOrder();

            // Coordinate fallback (prevents NullPointerException)
            double destLat = (order.getDeliveryLatitude() != null)
                    ? order.getDeliveryLatitude().doubleValue()
                    : 19.0760;
            double destLon = (order.getDeliveryLongitude() != null)
                    ? order.getDeliveryLongitude().doubleValue()
                    : 72.8777;

            // Distance calculation
            distanceKm = GeolocationUtil.calculateDistanceKm(
                    currentLocation.getLatitude().doubleValue(),
                    currentLocation.getLongitude().doubleValue(),
                    destLat,
                    destLon
            );

            // Estimated arrival
            estimatedMinutes = GeolocationUtil.estimateTimeMinutes(distanceKm);
            LocalDateTime estimatedArrival = LocalDateTime.now().plusMinutes(estimatedMinutes);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a");
            estimatedDeliveryTime = "Today by " + estimatedArrival.format(formatter);
        }

        return new DeliveryTrackingDto(
                delivery.getId(),
                delivery.getOrder().getId(),
                delivery.getDeliveryPerson() != null ? delivery.getDeliveryPerson().getUser().getName() : "Unassigned",
                delivery.getStatus().toString(),
                currentLocation,
                distanceKm,
                estimatedMinutes,
                estimatedDeliveryTime
        );
    }

    /**
     * Evicts live tracking data from Redis upon delivery completion or cancellation.
     */
    public void clearTrackingSession(Long deliveryId) {
        String redisKey = LOCATION_KEY_PREFIX + deliveryId;
        redisTemplate.delete(redisKey);
        log.info("Live tracking session evicted for delivery #{}", deliveryId);
    }
}