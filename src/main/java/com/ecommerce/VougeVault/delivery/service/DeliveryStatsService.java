package com.ecommerce.VougeVault.delivery.service;

import com.ecommerce.VougeVault.delivery.repository.DeliveryStatsRepository;
import com.ecommerce.VougeVault.delivery.dto.DeliveryStatsDto;
import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStats;
import com.ecommerce.VougeVault.delivery.repository.DeliveryPersonRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryStatsService {

    private final DeliveryStatsRepository statsRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;

    /**
     * Initialize stats when delivery person is created
     */
    @Transactional
    public void initializeStats(DeliveryPerson deliveryPerson) {
        DeliveryStats stats = new DeliveryStats();
        stats.setDeliveryPerson(deliveryPerson);
        stats.setTotalDeliveries(0);
        stats.setSuccessfulDeliveries(0);
        stats.setFailedDeliveries(0);
        stats.setTotalEarnings(BigDecimal.ZERO);
        stats.setUpdatedAt(LocalDateTime.now());

        statsRepository.save(stats);
        log.info("Stats initialized for delivery person {}", deliveryPerson.getId());
    }

    /**
     * Record successful delivery
     */
    @Transactional
    public void recordSuccessfulDelivery(Long deliveryPersonId, BigDecimal earnings) {
        DeliveryStats stats = statsRepository.findByDeliveryPersonId(deliveryPersonId)
                .orElseThrow(() -> new ResourceNotFoundException("Stats not found"));

        stats.setTotalDeliveries(stats.getTotalDeliveries() + 1);
        stats.setSuccessfulDeliveries(stats.getSuccessfulDeliveries() + 1);
        stats.setTotalEarnings(stats.getTotalEarnings().add(earnings));
        stats.setUpdatedAt(LocalDateTime.now());

        statsRepository.save(stats);
        log.info("Success recorded for delivery person {}: +{}", deliveryPersonId, earnings);
    }

    /**
     * Record failed delivery
     */
    @Transactional
    public void recordFailedDelivery(Long deliveryPersonId) {
        DeliveryStats stats = statsRepository.findByDeliveryPersonId(deliveryPersonId)
                .orElseThrow(() -> new ResourceNotFoundException("Stats not found"));

        stats.setTotalDeliveries(stats.getTotalDeliveries() + 1);
        stats.setFailedDeliveries(stats.getFailedDeliveries() + 1);
        stats.setUpdatedAt(LocalDateTime.now());

        statsRepository.save(stats);
        log.info("Failure recorded for delivery person {}", deliveryPersonId);
    }

    /**
     * Get delivery person stats
     */
    public DeliveryStatsDto getStats(Long deliveryPersonId) {
        DeliveryStats stats = statsRepository.findByDeliveryPersonId(deliveryPersonId)
                .orElseThrow(() -> new ResourceNotFoundException("Stats not found"));

        return new DeliveryStatsDto(
                stats.getTotalDeliveries(),
                stats.getSuccessfulDeliveries(),
                stats.getFailedDeliveries(),
                stats.successRate(),
                stats.getAverageRating(),
                stats.getTotalEarnings()
        );
    }

    /**
     * Update rating (called after customer reviews)
     */
    @Transactional
    public void updateAverageRating(Long deliveryPersonId, BigDecimal newRating) {
        DeliveryStats stats = statsRepository.findByDeliveryPersonId(deliveryPersonId)
                .orElseThrow(() -> new ResourceNotFoundException("Stats not found"));

        // Recalculate average (simplified - in production, query all reviews)
        if (stats.getAverageRating() == null) {
            stats.setAverageRating(newRating);
        } else {
            BigDecimal current = stats.getAverageRating().multiply(BigDecimal.valueOf(stats.getTotalDeliveries()));
            BigDecimal updated = current.add(newRating).divide(BigDecimal.valueOf(stats.getTotalDeliveries() + 1), 2, java.math.RoundingMode.HALF_UP);
            stats.setAverageRating(updated);
        }

        statsRepository.save(stats);
        log.info("Rating updated for delivery person {}: {}", deliveryPersonId, stats.getAverageRating());
    }
}