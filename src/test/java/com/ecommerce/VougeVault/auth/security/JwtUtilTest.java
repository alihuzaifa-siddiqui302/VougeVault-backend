package com.ecommerce.VougeVault.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    // Secure 256-bit key for HMAC-SHA256
    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long TEST_EXPIRATION = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject private @Value fields without loading the full Spring Context
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", TEST_EXPIRATION);
    }

    @Nested
    @DisplayName("Token Generation & Extraction")
    class GenerationAndExtractionTests {

        @Test
        @DisplayName("Should generate token and extract correct email subject")
        void generateToken_And_ExtractEmail() {
            String email = "test@example.com";
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "CUSTOMER");

            String token = jwtUtil.generateToken(email, claims);

            assertThat(token).isNotBlank();
            assertThat(jwtUtil.extractEmail(token)).isEqualTo(email);
        }

        @Test
        @DisplayName("Should extract custom claims correctly")
        void extractClaim_CustomClaims() {
            String email = "admin@example.com";
            Map<String, Object> claims = new HashMap<>();
            claims.put("role", "BRAND_ADMIN");
            claims.put("brandId", 42L);

            String token = jwtUtil.generateToken(email, claims);

            String extractedRole = jwtUtil.extractClaim(token, c -> c.get("role", String.class));
            Long extractedBrandId = jwtUtil.extractClaim(token, c -> c.get("brandId", Long.class));
            Date expiration = jwtUtil.extractClaim(token, Claims::getExpiration);

            assertThat(extractedRole).isEqualTo("BRAND_ADMIN");
            assertThat(extractedBrandId).isEqualTo(42L);
            assertThat(expiration).isAfter(new Date());
        }

        @Test
        @DisplayName("Should throw SignatureException when extracting claims from tampered token")
        void extractClaim_ShouldThrowException_WhenTokenTampered() {
            JwtUtil otherUtil = new JwtUtil();
            ReflectionTestUtils.setField(otherUtil, "secret", "9876543210abcdef9876543210abcdef9876543210abcdef9876543210abcdef");
            ReflectionTestUtils.setField(otherUtil, "expiration", TEST_EXPIRATION);

            String foreignToken = otherUtil.generateToken("user@example.com", new HashMap<>());

            assertThatThrownBy(() -> jwtUtil.extractEmail(foreignToken))
                    .isInstanceOf(SignatureException.class);
        }
    }

    @Nested
    @DisplayName("isTokenValid() Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return true for a valid, non-expired token")
        void isTokenValid_ShouldReturnTrue_WhenTokenIsValid() {
            String token = jwtUtil.generateToken("user@example.com", Map.of("role", "CUSTOMER"));

            boolean isValid = jwtUtil.isTokenValid(token);

            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false when token is expired")
        void isTokenValid_ShouldReturnFalse_WhenTokenIsExpired() {
            JwtUtil expiredUtil = new JwtUtil();
            ReflectionTestUtils.setField(expiredUtil, "secret", TEST_SECRET);
            ReflectionTestUtils.setField(expiredUtil, "expiration", -1000L); // Expired 1 second ago

            String expiredToken = expiredUtil.generateToken("user@example.com", new HashMap<>());

            boolean isValid = jwtUtil.isTokenValid(expiredToken);

            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false when token string is malformed or invalid")
        void isTokenValid_ShouldReturnFalse_WhenTokenIsMalformed() {
            assertThat(jwtUtil.isTokenValid("malformed.jwt.token")).isFalse();
            assertThat(jwtUtil.isTokenValid("")).isFalse();
            assertThat(jwtUtil.isTokenValid(null)).isFalse();
        }

        @Test
        @DisplayName("Should return false when token was signed with a different secret key")
        void isTokenValid_ShouldReturnFalse_WhenSignatureIsInvalid() {
            JwtUtil foreignUtil = new JwtUtil();
            ReflectionTestUtils.setField(foreignUtil, "secret", "differentSecretKeyDifferentSecretKey1234567890!");
            ReflectionTestUtils.setField(foreignUtil, "expiration", TEST_EXPIRATION);

            String foreignToken = foreignUtil.generateToken("user@example.com", new HashMap<>());

            boolean isValid = jwtUtil.isTokenValid(foreignToken);

            assertThat(isValid).isFalse();
        }
    }
}