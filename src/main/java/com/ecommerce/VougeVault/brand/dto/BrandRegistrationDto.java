package com.ecommerce.VougeVault.brand.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BrandRegistrationDto {

    @NotBlank(message="Brand Name is required")
    @Size(min=2, max=255)
    private String brandName;

    private String gstNumber;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 500, message = "Address must be between 5 and 500 characters")
    private String address;


    @NotBlank(message = "City is required")
    @Size(min = 2, max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(min = 2, max = 100)
    private String state;

    @NotBlank(message = "Pincode is required")
    @Size(min = 6, max = 10)
    private String pincode;

    @NotBlank(message = "Phone is required")
    @Size(min = 10, max = 15)
    private String phone;

    @NotBlank
    @Email(message = "Admin email must be valid")
    private String adminEmail;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String adminPassword;

    @NotBlank
    private String adminName;
}
