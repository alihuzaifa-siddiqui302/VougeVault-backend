package com.ecommerce.VougeVault.cart.repository;

import com.ecommerce.VougeVault.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);
    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);
    void deleteByCartId(Long cartId);
}