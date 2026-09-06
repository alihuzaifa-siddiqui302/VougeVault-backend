package com.ecommerce.VougeVault.auth.dto;

import com.ecommerce.VougeVault.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String role;
    private Long userId;
    private Long brandId;

}
