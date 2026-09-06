package com.ecommerce.VougeVault.auth.dto;

import com.ecommerce.VougeVault.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message="name is required")
    private String name;
    @NotBlank(message="email is required")
    @Email(message="email must be valid")
    private String email;
    @NotBlank(message="password is required")
    @Size(min=8,message="password must be atleast 8 characters")
    private String password;
    @NotNull(message="role is required")
    private Role role;
}
