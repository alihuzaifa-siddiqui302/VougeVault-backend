package com.ecommerce.VougeVault.delivery.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(nullable = false)
    private String otpCode;  // 6-digit code like "123456"

    @Column(name = "attempts_left")
    private Integer attemptsLeft = 3;  // User gets 3 tries

    @Column(name = "is_verified")
    private Boolean isVerified = false;  // Has OTP been verified?

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;  // When was it verified?

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;  // OTP valid for 15 minutes
}