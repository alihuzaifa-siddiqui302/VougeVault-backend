package com.ecommerce.VougeVault.wishlist.repository;

import com.ecommerce.VougeVault.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist,Long> {

    Optional<Wishlist> findByCustomerId(Long customerId);
}
