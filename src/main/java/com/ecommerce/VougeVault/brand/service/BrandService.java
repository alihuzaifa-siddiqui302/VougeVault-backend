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
import com.sun.jdi.request.DuplicateRequestException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRepository userRepository;
 //   private final PasswordEncoder passwordEncoder;

    public void registerBrand(BrandRegistrationDto dto){
      if(userRepository.existsByEmail(dto.getAdminEmail())){
          throw new DuplicateResourceException("email already registered");
      }
      Brand brand=new Brand();
      brand.setName(dto.getBrandName());
      brand.setGstNumber(dto.getGstNumber());
      brand.setStatus(BrandStatus.PENDING);
        brand.setAddress(dto.getAddress());
        brand.setCity(dto.getCity());
        brand.setState(dto.getState());
        brand.setPincode(dto.getPincode());
        brand.setPhone(dto.getPhone());
        brand.setEmail(dto.getAdminEmail());
      brandRepository.save(brand);

      User admin=new User();
      admin.setEmail(dto.getAdminEmail());
      admin.setPassword(dto.getAdminPassword());
      admin.setName(dto.getAdminName());
      admin.setRole(Role.BRAND_ADMIN);
      admin.setBrand(brand);
      userRepository.save(admin);

    }
    @Transactional
    public Brand updateBrand(Long brandId, BrandRegistrationDto dto, Long userId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

        // Verify ownership
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!brand.getId().equals(user.getBrand().getId())) {
            throw new AccessDeniedException("You do not have permission to update this brand");
        }

        brand.setName(dto.getBrandName());
        brand.setGstNumber(dto.getGstNumber());

        // Update address fields
        brand.setAddress(dto.getAddress());
        brand.setCity(dto.getCity());
        brand.setState(dto.getState());
        brand.setPincode(dto.getPincode());
        brand.setPhone(dto.getPhone());

        return brandRepository.save(brand);
    }
    @Transactional
    public void approveBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

        brand.setStatus(BrandStatus.APPROVED);
        brandRepository.save(brand);
    }
    @Transactional
    public void rejectBrand(Long brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        brand.setStatus(BrandStatus.REJECTED);
        brandRepository.save(brand);
    }
}
