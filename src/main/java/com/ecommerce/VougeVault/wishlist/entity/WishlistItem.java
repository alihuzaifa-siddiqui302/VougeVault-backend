package com.ecommerce.VougeVault.wishlist.entity;

import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name="wishlist_items")
public class WishlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="wishlist_id",nullable = false)
    private Wishlist wishlist;

    @ManyToOne
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;


}
