package com.ecommerce.VougeVault.catalog.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;  // Image ID: 1, 2, 3...

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;  // Which product does this image belong to?

    @Column(nullable = false)
    private String fileName;  // System name: "a1b2c3d4-e5f6-g7h8.jpg" (random UUID)

    @Column(nullable = false)
    private String filePath;  // Where to find it on disk: "uploads/products/a1b2c3d4-e5f6-g7h8.jpg"

    @Column(name = "original_file_name")
    private String originalFileName;  // What user uploaded: "my_shirt_photo.jpg"

    @Column(name = "file_size")
    private Long fileSize;  // Size in bytes: 2500000 (2.5MB)

    @Column(name = "mime_type")
    private String mimeType;  // File type: "image/jpeg", "image/png"

    @Column(name = "is_primary")
    private Boolean isPrimary = false;  // Is this the main image? true/false

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;  // When uploaded
}
