package com.ecommerce.VougeVault.wishlist.entity;

import com.ecommerce.VougeVault.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name="wishlist")
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="customer_id",nullable = false,unique = true)
    private User customer;

    @OneToMany(mappedBy = "wishlist", fetch = FetchType.LAZY,cascade=CascadeType.ALL,orphanRemoval = true)
    private List<WishlistItem> items;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
