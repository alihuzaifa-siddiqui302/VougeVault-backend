package com.ecommerce.VougeVault.wishlist.repository;

import com.ecommerce.VougeVault.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem,Long> {

    Optional<WishlistItem> findByWishlistIdAndProductVariantId(Long wishlistId,Long productVariantId);
    Optional<WishlistItem> findByIdAndWishlistId(Long Id,Long wishlistId);
}
