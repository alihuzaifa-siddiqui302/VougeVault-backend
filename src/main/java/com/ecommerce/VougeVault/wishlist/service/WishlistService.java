package com.ecommerce.VougeVault.wishlist.service;

import com.ecommerce.VougeVault.catalog.Repository.ProductVariantRepository;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import com.ecommerce.VougeVault.wishlist.dto.AddToWishlistDto;
import com.ecommerce.VougeVault.wishlist.dto.WishlistItemResponseDto;
import com.ecommerce.VougeVault.wishlist.dto.WishlistResponseDto;
import com.ecommerce.VougeVault.wishlist.entity.Wishlist;
import com.ecommerce.VougeVault.wishlist.entity.WishlistItem;
import com.ecommerce.VougeVault.wishlist.repository.WishlistItemRepository;
import com.ecommerce.VougeVault.wishlist.repository.WishlistRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final WishlistItemRepository wishlistItemRepository;
    @Transactional
    public Wishlist getOrCreateWishlist(Long customerId){
        return wishlistRepository.findByCustomerId(customerId)
                .orElseGet(()->{
                    User customer=userRepository.findById(customerId)
                            .orElseThrow(()->new RuntimeException("customer not found"));
                    Wishlist wishlist=new Wishlist();
                    wishlist.setCustomer(customer);
                    return wishlistRepository.save(wishlist);
                });
    }

    @Transactional
    public WishlistResponseDto addToWishlist(Long customerId,AddToWishlistDto dto){
        Wishlist wishlist=getOrCreateWishlist(customerId);
        ProductVariant variant=variantRepository.findById(dto.getProductVariantId())
                .orElseThrow(()->new RuntimeException("product variant not found"));

        Boolean alreadyExists =wishlistItemRepository.findByWishlistIdAndProductVariantId(wishlist.getId(), dto.getProductVariantId())
                .isPresent();
        if(!alreadyExists){
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProductVariant(variant);
            wishlistItemRepository.save(item);
        }
        return getWishlist(customerId);

    }

    public WishlistResponseDto getWishlist(Long customerId){
        Wishlist wishlist = getOrCreateWishlist(customerId);

        List<WishlistItemResponseDto> itemDtos = wishlist.getItems().stream()
                .map(this::toItemResponseDto)
                .collect(Collectors.toList());

        return new WishlistResponseDto(wishlist.getId(), itemDtos);
    }

    @Transactional
    public WishlistResponseDto removeFromWishlist(Long customerId, Long wishlistItemId) {
        Wishlist wishlist = getOrCreateWishlist(customerId);

        WishlistItem item = wishlistItemRepository.findByIdAndWishlistId(wishlistItemId, wishlist.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wishlist item not found"));

        wishlistItemRepository.delete(item);
        return getWishlist(customerId);
    }



    private WishlistItemResponseDto toItemResponseDto(WishlistItem item) {
        ProductVariant variant = item.getProductVariant();
        return new WishlistItemResponseDto(
                item.getId(),
                variant.getId(),
                variant.getProduct().getName(),
                variant.getSize(),
                variant.getColor(),
                variant.getPrice()
        );
    }

}
