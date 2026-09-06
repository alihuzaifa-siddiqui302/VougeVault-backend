package com.ecommerce.VougeVault.cart.service;

import com.ecommerce.VougeVault.cart.dto.AddToCartDto;
import com.ecommerce.VougeVault.cart.dto.CartItemResponseDto;
import com.ecommerce.VougeVault.cart.dto.CartResponseDto;
import com.ecommerce.VougeVault.cart.dto.UpdateAddToCartDto;
import com.ecommerce.VougeVault.cart.entity.Cart;
import com.ecommerce.VougeVault.cart.entity.CartItem;
import com.ecommerce.VougeVault.cart.repository.CartItemRepository;
import com.ecommerce.VougeVault.cart.repository.CartRepository;
import com.ecommerce.VougeVault.catalog.Repository.ProductVariantRepository;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository variantRepository;
    private final CartItemRepository cartItemRepository;

   @Transactional
    public Cart getOrCreateCart(Long customerId){
        return cartRepository.findByCustomerId(customerId)
                .orElseGet(()->{
                    User customer=userRepository.findById(customerId)
                            .orElseThrow(()->new RuntimeException("Customer not found"));
                    Cart cart=new Cart();
                    cart.setCustomer(customer);
                    return cartRepository.save(cart);
                });
    }
    @Transactional
    public CartResponseDto addToCart(Long customerId, AddToCartDto dto){
       Cart cart=getOrCreateCart(customerId);
        ProductVariant variant= variantRepository.findById(dto.getProductVariantId())
                .orElseThrow(()->new RuntimeException("product variant not found"));

        CartItem item=cartItemRepository.findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElse(null);
        if(item!=null){
            item.setQuantity(item.getQuantity()+ dto.getQuantity());
        }
        else{
            item=new CartItem();
            item.setCart(cart);
            item.setProductVariant(variant);
            item.setQuantity(dto.getQuantity());
        }
        cartItemRepository.save(item);
        return getCart(customerId);
    }
    public CartResponseDto getCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);

        List<CartItemResponseDto> itemDtos = cart.getItems().stream()
                .map(this::toItemResponseDto)
                .collect(Collectors.toList());

        BigDecimal total = itemDtos.stream()
                .map(CartItemResponseDto::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponseDto(cart.getId(), itemDtos, total);
    }

    @Transactional
    public CartResponseDto updateQuantity( Long customerId,Long cartItemId, UpdateAddToCartDto dto){
       Cart cart=getOrCreateCart(customerId);
       CartItem item=cartItemRepository.findByIdAndCartId(cartItemId,cart.getId())
               .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
       item.setQuantity(dto.getQuantity());
       cartItemRepository.save(item);
       return getCart(customerId);
    }
    @Transactional
    public CartResponseDto removeItem(Long customerId, Long cartItemId) {
        Cart cart = getOrCreateCart(customerId);

        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cartItemRepository.delete(item);
        return getCart(customerId);
    }

    @Transactional
    public void clearCart(Long customerId) {
        Cart cart = getOrCreateCart(customerId);
        cartItemRepository.deleteByCartId(cart.getId());
    }


     public CartItemResponseDto toItemResponseDto(CartItem item){
       ProductVariant variant=item.getProductVariant();
       BigDecimal subTotal=variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

         return new CartItemResponseDto(
                 item.getId(),
                 variant.getId(),
                 variant.getProduct().getName(),
                 variant.getSize(),
                 variant.getColor(),
                 variant.getPrice(),
                 item.getQuantity(),
                 subTotal
         );
     }
}

