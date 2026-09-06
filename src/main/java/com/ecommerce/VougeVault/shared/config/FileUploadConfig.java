package com.ecommerce.VougeVault.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "app.file-upload")
@Getter
@Setter
public class FileUploadConfig {
    private String uploadDir = "uploads/products";
    private long maxFileSize = 5242880; // 5MB
    private String allowedMimeTypes = "image/jpeg,image/png,image/webp,image/gif";

    public Path getUploadPath() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public boolean isAllowedMimeType(String mimeType) {
        return allowedMimeTypes.contains(mimeType);
    }

    public boolean isValidFileSize(long fileSize) {
        return fileSize > 0 && fileSize <= maxFileSize;
    }
}