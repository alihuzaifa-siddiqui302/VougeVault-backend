package com.ecommerce.VougeVault.delivery.service;

import com.ecommerce.VougeVault.delivery.dto.DeliveryStatsDto;
import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStats;
import com.ecommerce.VougeVault.delivery.repository.DeliveryPersonRepository;
import com.ecommerce.VougeVault.delivery.repository.DeliveryStatsRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryStatsServiceTest {

    @Mock
    private DeliveryStatsRepository statsRepository;

    @Mock
    private DeliveryPersonRepository deliveryPersonRepository;

    @InjectMocks
    private DeliveryStatsService deliveryStatsService;

    private DeliveryPerson sampleDeliveryPerson;
    private DeliveryStats sampleStats;

    private final Long deliveryPersonId = 1L;

    @BeforeEach
    void setUp() {
        sampleDeliveryPerson = new DeliveryPerson();
        sampleDeliveryPerson.setId(deliveryPersonId);

        sampleStats = new DeliveryStats();
        sampleStats.setId(10L);
        sampleStats.setDeliveryPerson(sampleDeliveryPerson);
        sampleStats.setTotalDeliveries(10);
        sampleStats.setSuccessfulDeliveries(8);
        sampleStats.setFailedDeliveries(2);
        sampleStats.setTotalEarnings(new BigDecimal("800.00"));
        sampleStats.setAverageRating(new BigDecimal("4.50"));
        sampleStats.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("initializeStats() Tests")
    class InitializeStatsTests {

        @Test
        @DisplayName("Should create, initialize zeroed statistics, and save stats for delivery person")
        void initializeStats_ShouldCreateAndSaveZeroedStats() {
            deliveryStatsService.initializeStats(sampleDeliveryPerson);

            ArgumentCaptor<DeliveryStats> statsCaptor = ArgumentCaptor.forClass(DeliveryStats.class);
            verify(statsRepository).save(statsCaptor.capture());
            DeliveryStats createdStats = statsCaptor.getValue();

            assertThat(createdStats.getDeliveryPerson()).isEqualTo(sampleDeliveryPerson);
            assertThat(createdStats.getTotalDeliveries()).isZero();
            assertThat(createdStats.getSuccessfulDeliveries()).isZero();
            assertThat(createdStats.getFailedDeliveries()).isZero();
            assertThat(createdStats.getTotalEarnings()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(createdStats.getUpdatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("recordSuccessfulDelivery() Tests")
    class RecordSuccessfulDeliveryTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery stats do not exist")
        void recordSuccessfulDelivery_ShouldThrowException_WhenStatsNotFound() {
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryStatsService.recordSuccessfulDelivery(deliveryPersonId, BigDecimal.valueOf(100)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Stats not found");

            verify(statsRepository, never()).save(any(DeliveryStats.class));
        }

        @Test
        @DisplayName("Should increment totalDeliveries, successfulDeliveries, and accumulate totalEarnings")
        void recordSuccessfulDelivery_ShouldIncrementCountsAndAddEarnings() {
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.of(sampleStats));

            deliveryStatsService.recordSuccessfulDelivery(deliveryPersonId, new BigDecimal("100.00"));

            assertThat(sampleStats.getTotalDeliveries()).isEqualTo(11);
            assertThat(sampleStats.getSuccessfulDeliveries()).isEqualTo(9);
            assertThat(sampleStats.getFailedDeliveries()).isEqualTo(2);
            assertThat(sampleStats.getTotalEarnings()).isEqualByComparingTo("900.00");
            verify(statsRepository).save(sampleStats);
        }
    }

    @Nested
    @DisplayName("recordFailedDelivery() Tests")
    class RecordFailedDeliveryTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when delivery stats do not exist")
        void recordFailedDelivery_ShouldThrowException_WhenStatsNotFound() {
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryStatsService.recordFailedDelivery(deliveryPersonId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Stats not found");

            verify(statsRepository, never()).save(any(DeliveryStats.class));
        }

        @Test
        @DisplayName("Should increment totalDeliveries and failedDeliveries without changing earnings")
        void recordFailedDelivery_ShouldIncrementFailedCount_WithoutChangingEarnings() {
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.of(sampleStats));

            deliveryStatsService.recordFailedDelivery(deliveryPersonId);

            assertThat(sampleStats.getTotalDeliveries()).isEqualTo(11);
            assertThat(sampleStats.getSuccessfulDeliveries()).isEqualTo(8);
            assertThat(sampleStats.getFailedDeliveries()).isEqualTo(3);
            assertThat(sampleStats.getTotalEarnings()).isEqualByComparingTo("800.00");
            verify(statsRepository).save(sampleStats);
        }
    }

    @Nested
    @DisplayName("getStats() Tests")
    class GetStatsTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when stats are missing")
        void getStats_ShouldThrowException_WhenStatsNotFound() {
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryStatsService.getStats(deliveryPersonId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Stats not found");
        }

        @Test
        @DisplayName("Should map DeliveryStats entity correctly to DeliveryStatsDto")
        void getStats_ShouldReturnMappedDto_WhenFound() {
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.of(sampleStats));

            DeliveryStatsDto dto = deliveryStatsService.getStats(deliveryPersonId);

            assertThat(dto).isNotNull();
            assertThat(dto.getTotalDeliveries()).isEqualTo(10);
            assertThat(dto.getSuccessfulDeliveries()).isEqualTo(8);
            assertThat(dto.getFailedDeliveries()).isEqualTo(2);
            assertThat(dto.getAverageRating()).isEqualByComparingTo("4.50");
            assertThat(dto.getTotalEarnings()).isEqualByComparingTo("800.00");
        }
    }

    @Nested
    @DisplayName("updateAverageRating() Tests")
    class UpdateAverageRatingTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when stats are missing")
        void updateAverageRating_ShouldThrowException_WhenStatsNotFound() {
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deliveryStatsService.updateAverageRating(deliveryPersonId, new BigDecimal("5.00")))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Stats not found");

            verify(statsRepository, never()).save(any(DeliveryStats.class));
        }

        @Test
        @DisplayName("Should directly set averageRating when current averageRating is null")
        void updateAverageRating_ShouldSetRatingDirectly_WhenInitialRatingIsNull() {
            sampleStats.setAverageRating(null);
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.of(sampleStats));

            deliveryStatsService.updateAverageRating(deliveryPersonId, new BigDecimal("4.80"));

            assertThat(sampleStats.getAverageRating()).isEqualByComparingTo("4.80");
            verify(statsRepository).save(sampleStats);
        }

        @Test
        @DisplayName("Should correctly recalculate weighted average rating when averageRating already exists")
        void updateAverageRating_ShouldRecalculateWeightedAverage_WhenRatingExists() {
            // initial averageRating = 4.50, totalDeliveries = 10 -> current = 45.00
            // new rating = 5.00 -> updated = (45.00 + 5.00) / 11 = 50.00 / 11 = 4.55 (HALF_UP)
            sampleStats.setAverageRating(new BigDecimal("4.50"));
            sampleStats.setTotalDeliveries(10);
            when(statsRepository.findByDeliveryPersonId(deliveryPersonId)).thenReturn(Optional.of(sampleStats));

            deliveryStatsService.updateAverageRating(deliveryPersonId, new BigDecimal("5.00"));

            assertThat(sampleStats.getAverageRating()).isEqualByComparingTo("4.55");
            verify(statsRepository).save(sampleStats);
        }
    }
}