package com.ecommerce.VougeVault.brand.service;

import com.ecommerce.VougeVault.brand.dto.BrandRegistrationDto;
import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.brand.entity.BrandStatus;
import com.ecommerce.VougeVault.brand.repository.BrandRepository;
import com.ecommerce.VougeVault.shared.exception.AccessDeniedException;
import com.ecommerce.VougeVault.shared.exception.DuplicateResourceException;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BrandService brandService;

    private BrandRegistrationDto registrationDto;
    private Brand sampleBrand;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        registrationDto = new BrandRegistrationDto();
        registrationDto.setBrandName("Vouge Apparel");
        registrationDto.setGstNumber("GSTIN123456");
        registrationDto.setAddress("123 Fashion Street");
        registrationDto.setCity("Mumbai");
        registrationDto.setState("Maharashtra");
        registrationDto.setPincode("400001");
        registrationDto.setPhone("9876543210");
        registrationDto.setAdminEmail("admin@vouge.com");
        registrationDto.setAdminPassword("rawPassword123");
        registrationDto.setAdminName("Jane Doe");

        sampleBrand = new Brand();
        sampleBrand.setId(10L);
        sampleBrand.setName("Vouge Apparel");
        sampleBrand.setGstNumber("GSTIN123456");
        sampleBrand.setStatus(BrandStatus.PENDING);

        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setEmail("admin@vouge.com");
        sampleUser.setName("Jane Doe");
        sampleUser.setRole(Role.BRAND_ADMIN);
        sampleUser.setBrand(sampleBrand);
    }

    @Nested
    @DisplayName("registerBrand() Tests")
    class RegisterBrandTests {

        @Test
        @DisplayName("Should throw DuplicateResourceException when admin email is already registered")
        void registerBrand_ShouldThrowException_WhenEmailAlreadyExists() {
            when(userRepository.existsByEmail(registrationDto.getAdminEmail())).thenReturn(true);

            assertThatThrownBy(() -> brandService.registerBrand(registrationDto))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessage("email already registered");

            verify(brandRepository, never()).save(any(Brand.class));
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should save brand with PENDING status and save user with BRAND_ADMIN role")
        void registerBrand_ShouldSaveBrandAndUser_WhenValid() {
            when(userRepository.existsByEmail(registrationDto.getAdminEmail())).thenReturn(false);

            brandService.registerBrand(registrationDto);

            // Capture and verify Brand
            ArgumentCaptor<Brand> brandCaptor = ArgumentCaptor.forClass(Brand.class);
            verify(brandRepository).save(brandCaptor.capture());
            Brand savedBrand = brandCaptor.getValue();

            assertThat(savedBrand.getName()).isEqualTo("Vouge Apparel");
            assertThat(savedBrand.getGstNumber()).isEqualTo("GSTIN123456");
            assertThat(savedBrand.getStatus()).isEqualTo(BrandStatus.PENDING);
            assertThat(savedBrand.getEmail()).isEqualTo("admin@vouge.com");

            // Capture and verify User
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo("admin@vouge.com");
            assertThat(savedUser.getPassword()).isEqualTo("rawPassword123");
            assertThat(savedUser.getName()).isEqualTo("Jane Doe");
            assertThat(savedUser.getRole()).isEqualTo(Role.BRAND_ADMIN);
            assertThat(savedUser.getBrand()).isEqualTo(savedBrand);
        }
    }

    @Nested
    @DisplayName("updateBrand() Tests")
    class UpdateBrandTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when brand is not found")
        void updateBrand_ShouldThrowException_WhenBrandNotFound() {
            when(brandRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.updateBrand(10L, registrationDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Brand not found");

            verify(userRepository, never()).findById(anyLong());
            verify(brandRepository, never()).save(any(Brand.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when user is not found")
        void updateBrand_ShouldThrowException_WhenUserNotFound() {
            when(brandRepository.findById(10L)).thenReturn(Optional.of(sampleBrand));
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.updateBrand(10L, registrationDto, 1L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("User not found");

            verify(brandRepository, never()).save(any(Brand.class));
        }

        @Test
        @DisplayName("Should throw AccessDeniedException when user does not own the brand")
        void updateBrand_ShouldThrowException_WhenUserDoesNotOwnBrand() {
            Brand differentBrand = new Brand();
            differentBrand.setId(99L);
            sampleUser.setBrand(differentBrand);

            when(brandRepository.findById(10L)).thenReturn(Optional.of(sampleBrand));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

            assertThatThrownBy(() -> brandService.updateBrand(10L, registrationDto, 1L))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("You do not have permission to update this brand");

            verify(brandRepository, never()).save(any(Brand.class));
        }

        @Test
        @DisplayName("Should update brand fields and return updated brand when caller is owner")
        void updateBrand_ShouldUpdateAndReturnBrand_WhenAuthorized() {
            when(brandRepository.findById(10L)).thenReturn(Optional.of(sampleBrand));
            when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
            when(brandRepository.save(any(Brand.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Brand updated = brandService.updateBrand(10L, registrationDto, 1L);

            assertThat(updated).isNotNull();
            assertThat(updated.getName()).isEqualTo("Vouge Apparel");
            assertThat(updated.getCity()).isEqualTo("Mumbai");
            assertThat(updated.getState()).isEqualTo("Maharashtra");
            verify(brandRepository).save(sampleBrand);
        }
    }

    @Nested
    @DisplayName("approveBrand() Tests")
    class ApproveBrandTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when approving non-existent brand")
        void approveBrand_ShouldThrowException_WhenNotFound() {
            when(brandRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.approveBrand(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Brand not found");
        }

        @Test
        @DisplayName("Should change brand status to APPROVED and save")
        void approveBrand_ShouldSetStatusApproved_WhenFound() {
            when(brandRepository.findById(10L)).thenReturn(Optional.of(sampleBrand));

            brandService.approveBrand(10L);

            assertThat(sampleBrand.getStatus()).isEqualTo(BrandStatus.APPROVED);
            verify(brandRepository).save(sampleBrand);
        }
    }

    @Nested
    @DisplayName("rejectBrand() Tests")
    class RejectBrandTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when rejecting non-existent brand")
        void rejectBrand_ShouldThrowException_WhenNotFound() {
            when(brandRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> brandService.rejectBrand(10L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Brand not found");
        }

        @Test
        @DisplayName("Should change brand status to REJECTED and save")
        void rejectBrand_ShouldSetStatusRejected_WhenFound() {
            when(brandRepository.findById(10L)).thenReturn(Optional.of(sampleBrand));

            brandService.rejectBrand(10L);

            assertThat(sampleBrand.getStatus()).isEqualTo(BrandStatus.REJECTED);
            verify(brandRepository).save(sampleBrand);
        }
    }
}