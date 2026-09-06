package com.ecommerce.VougeVault.catalog.entity;

import com.ecommerce.VougeVault.brand.entity.Brand;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.sql.results.graph.Fetch;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name="categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch =FetchType.LAZY)
    @JoinColumn(name="brand_id",nullable = false)
    private Brand brand;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
