package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.shared.config.FileUploadConfig;
import com.ecommerce.VougeVault.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileUploadConfig fileUploadConfig;

    public String storeFile(MultipartFile file, Long productId) throws IOException {
        // Validate file
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        if (!fileUploadConfig.isAllowedMimeType(file.getContentType())) {
            throw new BusinessException("File type not allowed. Allowed types: " + fileUploadConfig.getAllowedMimeTypes());
        }

        if (!fileUploadConfig.isValidFileSize(file.getSize())) {
            throw new BusinessException("File size exceeds maximum allowed size of " + (fileUploadConfig.getMaxFileSize() / 1024 / 1024) + "MB");
        }

        // Create directory if not exists
        Path uploadPath = fileUploadConfig.getUploadPath();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created upload directory: {}", uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        // Store file
        Path filePath = uploadPath.resolve(storedFileName);
        Files.copy(file.getInputStream(), filePath);

        log.info("File stored successfully: {} for product {}", storedFileName, productId);
        return storedFileName;
    }

    public byte[] retrieveFile(String fileName) throws IOException {
        Path filePath = fileUploadConfig.getUploadPath().resolve(fileName).normalize();

        // Prevent directory traversal attacks
        if (!filePath.getParent().equals(fileUploadConfig.getUploadPath())) {
            throw new BusinessException("Invalid file path");
        }

        if (!Files.exists(filePath)) {
            throw new BusinessException("File not found: " + fileName);
        }

        return Files.readAllBytes(filePath);
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = fileUploadConfig.getUploadPath().resolve(fileName).normalize();

            // Prevent directory traversal attacks
            if (!filePath.getParent().equals(fileUploadConfig.getUploadPath())) {
                throw new BusinessException("Invalid file path");
            }

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted successfully: {}", fileName);
            }
        } catch (IOException e) {
            log.error("Error deleting file {}: {}", fileName, e.getMessage());
            throw new BusinessException("Failed to delete file: " + e.getMessage());
        }
    }
}