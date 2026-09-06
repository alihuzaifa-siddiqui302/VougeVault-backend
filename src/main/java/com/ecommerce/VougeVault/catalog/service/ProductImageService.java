package com.ecommerce.VougeVault.catalog.service;

import com.ecommerce.VougeVault.catalog.dto.ProductImageDto;
import com.ecommerce.VougeVault.catalog.dto.UploadImageResponseDto;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductImage;
import com.ecommerce.VougeVault.catalog.Repository.ProductImageRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductRepository;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService {

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;

    @Value("${app.api-base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Transactional
    public UploadImageResponseDto uploadImage(Long productId, MultipartFile file, Boolean isPrimary) throws IOException {
        // Verify product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // Store file
        String storedFileName = fileStorageService.storeFile(file, productId);

        // If this is marked as primary, unset other primaries
        if (isPrimary != null && isPrimary) {
            productImageRepository.findByProductId(productId).forEach(img -> {
                if (img.getIsPrimary()) {
                    img.setIsPrimary(false);
                    productImageRepository.save(img);
                }
            });
        }

        // Create image entity
        ProductImage productImage = new ProductImage();
        productImage.setProduct(product);
        productImage.setFileName(storedFileName);
        productImage.setFilePath(storedFileName);
        productImage.setOriginalFileName(file.getOriginalFilename());
        productImage.setFileSize(file.getSize());
        productImage.setMimeType(file.getContentType());
        productImage.setIsPrimary(isPrimary != null ? isPrimary : false);

        productImage = productImageRepository.save(productImage);

        log.info("Image uploaded for product {}: {}", productId, storedFileName);

        String downloadUrl = apiBaseUrl + "/api/products/images/download/" + storedFileName;
        return new UploadImageResponseDto(
                productImage.getId(),
                storedFileName,
                downloadUrl,
                productImage.getIsPrimary(),
                productImage.getFileSize()
        );
    }

    public List<ProductImageDto> getProductImages(Long productId) {
        // Verify product exists
        productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        return productImageRepository.findImagesByProductIdOrderByCreated(productId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public ProductImageDto getPrimaryImage(Long productId) {
        ProductImage image = productImageRepository.findPrimaryImageByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Primary image not found for product"));

        return toDto(image);
    }

    public byte[] downloadImage(String fileName) throws IOException {
        return fileStorageService.retrieveFile(fileName);
    }

    @Transactional
    public void deleteImage(Long imageId, Long brandId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        // Verify brand owns the product
        if (!image.getProduct().getBrand().getId().equals(brandId)) {
            throw new ResourceNotFoundException("Image not found");
        }

        // Delete file
        fileStorageService.deleteFile(image.getFileName());

        // Delete from database
        productImageRepository.deleteById(imageId);

        log.info("Image deleted: {}", imageId);
    }

    @Transactional
    public void setPrimaryImage(Long imageId, Long brandId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        // Verify brand owns the product
        if (!image.getProduct().getBrand().getId().equals(brandId)) {
            throw new ResourceNotFoundException("Image not found");
        }

        // Unset other primaries for this product
        productImageRepository.findByProductId(image.getProduct().getId()).forEach(img -> {
            if (img.getIsPrimary()) {
                img.setIsPrimary(false);
                productImageRepository.save(img);
            }
        });

        // Set this as primary
        image.setIsPrimary(true);
        productImageRepository.save(image);

        log.info("Primary image set: {}", imageId);
    }

    private ProductImageDto toDto(ProductImage image) {
        String downloadUrl = apiBaseUrl + "/api/products/images/download/" + image.getFileName();
        return new ProductImageDto(
                image.getId(),
                image.getFileName(),
                downloadUrl,
                image.getFileSize(),
                image.getMimeType(),
                image.getIsPrimary(),
                image.getCreatedAt()
        );
    }
}