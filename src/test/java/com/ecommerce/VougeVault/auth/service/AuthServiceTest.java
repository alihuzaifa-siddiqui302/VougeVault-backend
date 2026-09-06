package com.ecommerce.VougeVault.auth.service;

import com.ecommerce.VougeVault.auth.dto.LoginRequest;
import com.ecommerce.VougeVault.auth.dto.LoginResponse;
import com.ecommerce.VougeVault.auth.dto.RegisterRequest;
import com.ecommerce.VougeVault.auth.security.JwtUtil;
import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.brand.entity.BrandStatus;
import com.ecommerce.VougeVault.shared.exception.DuplicateResourceException;
import com.ecommerce.VougeVault.user.entity.Role;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setName("Jane Doe");
        registerRequest.setPassword("rawPassword");
        registerRequest.setRole(Role.CUSTOMER);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("rawPassword");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Jane Doe");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.CUSTOMER);
    }

    @Nested
    @DisplayName("register() Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should throw DuplicateResourceException when email already exists")
        void register_ShouldThrowException_WhenEmailAlreadyExists() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("email already registered");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when role is not CUSTOMER or DELIVERY_PERSON")
        void register_ShouldThrowException_WhenRoleIsInvalidForSelfRegistration() {
            registerRequest.setRole(Role.BRAND_ADMIN);
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);

            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid role for self-registration");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should encode password and save user when request is valid")
        void register_ShouldSaveUser_WhenRequestIsValid() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");

            authService.register(registerRequest);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
            assertThat(savedUser.getName()).isEqualTo("Jane Doe");
            assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
            assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER);
        }

        @Test
        @DisplayName("Should successfully register when role is DELIVERY_PERSON")
        void register_ShouldSaveUser_WhenRoleIsDeliveryPerson() {
            registerRequest.setRole(Role.DELIVERY_PERSON);
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

            authService.register(registerRequest);

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());

            assertThat(userCaptor.getValue().getRole()).isEqualTo(Role.DELIVERY_PERSON);
        }
    }

    @Nested
    @DisplayName("Login() Tests")
    class LoginTests {

        @Test
        @DisplayName("Should propagate exception when authentication fails")
        void login_ShouldThrowException_WhenAuthenticationFails() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.Login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Bad credentials");

            verify(userRepository, never()).findByEmail(anyString());
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user is not found after authentication")
        void login_ShouldThrowAccessDeniedException_WhenUserNotFound() {
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.Login(loginRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when BRAND_ADMIN has no brand attached")
        void login_ShouldThrowAccessDeniedException_WhenBrandAdminHasNoBrand() {
            testUser.setRole(Role.BRAND_ADMIN);
            testUser.setBrand(null);
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.Login(loginRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Your brand registration is pending approval");
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when BRAND_ADMIN brand status is not APPROVED")
        void login_ShouldThrowAccessDeniedException_WhenBrandStatusIsNotApproved() {
            Brand brand = new Brand();
            brand.setId(10L);
            brand.setStatus(BrandStatus.PENDING);

            testUser.setRole(Role.BRAND_ADMIN);
            testUser.setBrand(brand);
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.Login(loginRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("Your brand registration is pending approval");
        }

        @Test
        @DisplayName("Should return LoginResponse without brandId for regular customer")
        void login_ShouldReturnLoginResponse_ForCustomer() {
            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(jwtUtil.generateToken(eq(testUser.getEmail()), anyMap())).thenReturn("mocked-jwt-token");

            LoginResponse response = authService.Login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("mocked-jwt-token");
            assertThat(response.getRole()).isEqualTo("CUSTOMER");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getBrandId()).isNull();

            verify(jwtUtil).generateToken(eq("test@example.com"), argThat(claims ->
                    claims.get("role").equals("CUSTOMER") &&
                            claims.get("userId").equals(1L) &&
                            !claims.containsKey("brandId")
            ));
        }

        @Test
        @DisplayName("Should return LoginResponse with brandId for approved BRAND_ADMIN")
        void login_ShouldReturnLoginResponse_ForApprovedBrandAdmin() {
            Brand brand = new Brand();
            brand.setId(55L);
            brand.setStatus(BrandStatus.APPROVED);

            testUser.setRole(Role.BRAND_ADMIN);
            testUser.setBrand(brand);

            when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
            when(jwtUtil.generateToken(eq(testUser.getEmail()), anyMap())).thenReturn("brand-admin-token");

            LoginResponse response = authService.Login(loginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("brand-admin-token");
            assertThat(response.getRole()).isEqualTo("BRAND_ADMIN");
            assertThat(response.getUserId()).isEqualTo(1L);
            assertThat(response.getBrandId()).isEqualTo(55L);

            verify(jwtUtil).generateToken(eq("test@example.com"), argThat(claims ->
                    claims.get("role").equals("BRAND_ADMIN") &&
                            claims.get("userId").equals(1L) &&
                            claims.get("brandId").equals(55L)
            ));
        }
    }
}