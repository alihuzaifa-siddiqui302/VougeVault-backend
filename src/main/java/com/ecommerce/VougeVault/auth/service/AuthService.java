package com.ecommerce.VougeVault.auth.service;

import com.ecommerce.VougeVault.auth.dto.LoginRequest;
import com.ecommerce.VougeVault.auth.dto.LoginResponse;
import com.ecommerce.VougeVault.auth.dto.RegisterRequest;
import com.ecommerce.VougeVault.auth.security.JwtUtil;
import com.ecommerce.VougeVault.brand.entity.BrandStatus;
import com.ecommerce.VougeVault.shared.exception.DuplicateResourceException;
import com.ecommerce.VougeVault.user.entity.Role;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
   private final UserRepository userRepository;
   private final PasswordEncoder passwordEncoder;
   private final AuthenticationManager authenticationManager;
   private final JwtUtil jwtUtil;

    public void register(RegisterRequest request){
       if(userRepository.existsByEmail(request.getEmail())){
           throw new DuplicateResourceException("email already registered");
       }
       if(request.getRole()!= Role.CUSTOMER && request.getRole()!= Role.DELIVERY_PERSON){
           throw new IllegalArgumentException("Invalid role for self-registration");
       }
       User user=new User();
       user.setEmail(request.getEmail());
       user.setName(request.getName());
       user.setPassword(passwordEncoder.encode(request.getPassword()));
       user.setRole(request.getRole());
       userRepository.save(user);


    }

    public LoginResponse Login(LoginRequest request){
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(),request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AccessDeniedException("Invalid credentials"));

        if(user.getRole()==Role.BRAND_ADMIN) {
            if (user.getBrand() == null || user.getBrand().getStatus() != BrandStatus.APPROVED) {
                throw new AccessDeniedException("Your brand registration is pending approval");
            }
        }
            Map<String,Object> claims=new HashMap<>();
             claims.put("role",user.getRole().name());
             claims.put("userId",user.getId());
             if(user.getBrand()!=null){
                 claims.put("brandId",user.getBrand().getId());
             }
             String token= jwtUtil.generateToken(user.getEmail(), claims);
             Long brandId=user.getBrand()!=null ? user.getBrand().getId() : null;

             return new LoginResponse(token,user.getRole().name(),user.getId(),brandId);
        }

}
