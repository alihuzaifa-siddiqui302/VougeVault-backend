package com.ecommerce.VougeVault.delivery.entity;

import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_person_id", nullable = false)
    private DeliveryPerson deliveryPerson;

    @Column(name = "total_deliveries")
    private Integer totalDeliveries = 0;

    @Column(name = "successful_deliveries")
    private Integer successfulDeliveries = 0;

    @Column(name = "failed_deliveries")
    private Integer failedDeliveries = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;  // 4.5 stars

    @Column(name = "total_earnings", precision = 10, scale = 2)
    private BigDecimal totalEarnings = BigDecimal.ZERO;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public Double successRate() {
        if (totalDeliveries == 0) return 0.0;
        return (successfulDeliveries * 100.0) / totalDeliveries;
    }
}