package com.ecommerce.VougeVault.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadImageResponseDto {
    private Long imageId;
    private String fileName;
    private String downloadUrl;
    private Boolean isPrimary;
    private Long fileSize;
}