package com.ecommerce.VougeVault.catalog.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProductImageDto {
    private Long imageId;
    private String fileName;
    private String downloadUrl;
    private Long fileSize;
    private String mimeType;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}

