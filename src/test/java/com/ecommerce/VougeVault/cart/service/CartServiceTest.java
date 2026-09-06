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
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.shared.exception.ResourceNotFoundException;
import com.ecommerce.VougeVault.user.entity.User;
import com.ecommerce.VougeVault.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    private User sampleCustomer;
    private Product sampleProduct;
    private ProductVariant sampleVariant;
    private Cart sampleCart;
    private CartItem sampleCartItem;
    private AddToCartDto addToCartDto;

    private final Long customerId = 1L;
    private final Long cartId = 10L;
    private final Long variantId = 100L;
    private final Long cartItemId = 500L;

    @BeforeEach
    void setUp() {
        sampleCustomer = new User();
        sampleCustomer.setId(customerId);
        sampleCustomer.setEmail("customer@example.com");

        sampleProduct = new Product();
        sampleProduct.setId(50L);
        sampleProduct.setName("Tailored Linen Blazer");

        sampleVariant = new ProductVariant();
        sampleVariant.setId(variantId);
        sampleVariant.setSize("L");
        sampleVariant.setColor("Navy");
        sampleVariant.setPrice(new BigDecimal("120.00"));
        sampleVariant.setProduct(sampleProduct);

        sampleCart = new Cart();
        sampleCart.setId(cartId);
        sampleCart.setCustomer(sampleCustomer);
        sampleCart.setItems(new ArrayList<>());

        sampleCartItem = new CartItem();
        sampleCartItem.setId(cartItemId);
        sampleCartItem.setCart(sampleCart);
        sampleCartItem.setProductVariant(sampleVariant);
        sampleCartItem.setQuantity(2);

        addToCartDto = new AddToCartDto();
        addToCartDto.setProductVariantId(variantId);
        addToCartDto.setQuantity(3);
    }

    @Nested
    @DisplayName("getOrCreateCart() Tests")
    class GetOrCreateCartTests {

        @Test
        @DisplayName("Should return existing cart when found for customer")
        void getOrCreateCart_ShouldReturnExisting_WhenCartExists() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));

            Cart result = cartService.getOrCreateCart(customerId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(cartId);
            verify(userRepository, never()).findById(anyLong());
            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when cart does not exist and customer is not found")
        void getOrCreateCart_ShouldThrowException_WhenCustomerNotFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
            when(userRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.getOrCreateCart(customerId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Customer not found");

            verify(cartRepository, never()).save(any(Cart.class));
        }

        @Test
        @DisplayName("Should create and save a new cart when not found but customer exists")
        void getOrCreateCart_ShouldCreateAndSave_WhenCustomerFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
                Cart c = invocation.getArgument(0);
                c.setId(cartId);
                return c;
            });

            Cart result = cartService.getOrCreateCart(customerId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(cartId);
            assertThat(result.getCustomer()).isEqualTo(sampleCustomer);
            verify(cartRepository).save(any(Cart.class));
        }
    }

    @Nested
    @DisplayName("addToCart() Tests")
    class AddToCartTests {

        @Test
        @DisplayName("Should throw RuntimeException when product variant does not exist")
        void addToCart_ShouldThrowException_WhenVariantNotFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(variantRepository.findById(variantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.addToCart(customerId, addToCartDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("product variant not found");

            verify(cartItemRepository, never()).save(any(CartItem.class));
        }

        @Test
        @DisplayName("Should create and save a new CartItem when variant is not yet in cart")
        void addToCart_ShouldCreateNewItem_WhenItemNotInCart() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variantId)).thenReturn(Optional.empty());

            CartResponseDto response = cartService.addToCart(customerId, addToCartDto);

            ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository).save(itemCaptor.capture());

            CartItem savedItem = itemCaptor.getValue();
            assertThat(savedItem.getCart()).isEqualTo(sampleCart);
            assertThat(savedItem.getProductVariant()).isEqualTo(sampleVariant);
            assertThat(savedItem.getQuantity()).isEqualTo(3);
            assertThat(response.getCartId()).isEqualTo(cartId);
        }

        @Test
        @DisplayName("Should increment quantity and save existing CartItem when variant already in cart")
        void addToCart_ShouldIncrementQuantity_WhenItemAlreadyInCart() {
            sampleCartItem.setQuantity(2);

            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(cartItemRepository.findByCartIdAndProductVariantId(cartId, variantId)).thenReturn(Optional.of(sampleCartItem));

            CartResponseDto response = cartService.addToCart(customerId, addToCartDto);

            ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository).save(itemCaptor.capture());

            CartItem savedItem = itemCaptor.getValue();
            assertThat(savedItem.getId()).isEqualTo(cartItemId);
            assertThat(savedItem.getQuantity()).isEqualTo(5); // 2 existing + 3 new
            assertThat(response.getCartId()).isEqualTo(cartId);
        }
    }

    @Nested
    @DisplayName("getCart() Tests")
    class GetCartTests {

        @Test
        @DisplayName("Should calculate item subtotals and sum total correctly for populated cart")
        void getCart_ShouldCalculateTotals_WhenCartHasItems() {
            ProductVariant secondVariant = new ProductVariant();
            secondVariant.setId(101L);
            secondVariant.setSize("M");
            secondVariant.setColor("White");
            secondVariant.setPrice(new BigDecimal("30.00"));
            secondVariant.setProduct(sampleProduct);

            CartItem secondItem = new CartItem();
            secondItem.setId(501L);
            secondItem.setCart(sampleCart);
            secondItem.setProductVariant(secondVariant);
            secondItem.setQuantity(1);

            sampleCart.setItems(List.of(sampleCartItem, secondItem)); // Item 1: 120.00 * 2 = 240.00; Item 2: 30.00 * 1 = 30.00
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));

            CartResponseDto response = cartService.getCart(customerId);

            assertThat(response).isNotNull();
            assertThat(response.getCartId()).isEqualTo(cartId);
            assertThat(response.getItems()).hasSize(2);

            CartItemResponseDto firstItemDto = response.getItems().get(0);
            assertThat(firstItemDto.getCartItemId()).isEqualTo(cartItemId);
            assertThat(firstItemDto.getProductName()).isEqualTo("Tailored Linen Blazer");
            assertThat(firstItemDto.getSubtotal()).isEqualByComparingTo("240.00");

            CartItemResponseDto secondItemDto = response.getItems().get(1);
            assertThat(secondItemDto.getCartItemId()).isEqualTo(501L);
            assertThat(secondItemDto.getSubtotal()).isEqualByComparingTo("30.00");

            assertThat(response.getTotalPrice()).isEqualByComparingTo("270.00");
        }

        @Test
        @DisplayName("Should return zero total and empty item list when cart has no items")
        void getCart_ShouldReturnZeroTotal_WhenCartIsEmpty() {
            sampleCart.setItems(Collections.emptyList());
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));

            CartResponseDto response = cartService.getCart(customerId);

            assertThat(response.getCartId()).isEqualTo(cartId);
            assertThat(response.getItems()).isEmpty();
            assertThat(response.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("updateQuantity() Tests")
    class UpdateQuantityTests {

        private UpdateAddToCartDto updateDto;

        @BeforeEach
        void initUpdateDto() {
            updateDto = new UpdateAddToCartDto();
            updateDto.setQuantity(8);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when cart item is not found in cart")
        void updateQuantity_ShouldThrowException_WhenCartItemNotFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(cartItemRepository.findByIdAndCartId(cartItemId, cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateQuantity(customerId, cartItemId, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart item not found");

            verify(cartItemRepository, never()).save(any(CartItem.class));
        }

        @Test
        @DisplayName("Should update item quantity and save when cart item is found")
        void updateQuantity_ShouldUpdateAndSave_WhenItemFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(cartItemRepository.findByIdAndCartId(cartItemId, cartId)).thenReturn(Optional.of(sampleCartItem));

            CartResponseDto response = cartService.updateQuantity(customerId, cartItemId, updateDto);

            ArgumentCaptor<CartItem> itemCaptor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository).save(itemCaptor.capture());

            assertThat(itemCaptor.getValue().getQuantity()).isEqualTo(8);
            assertThat(response.getCartId()).isEqualTo(cartId);
        }
    }

    @Nested
    @DisplayName("removeItem() Tests")
    class RemoveItemTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when cart item to remove is not found")
        void removeItem_ShouldThrowException_WhenCartItemNotFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(cartItemRepository.findByIdAndCartId(cartItemId, cartId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.removeItem(customerId, cartItemId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart item not found");

            verify(cartItemRepository, never()).delete(any(CartItem.class));
        }

        @Test
        @DisplayName("Should delete cart item and return updated cart response")
        void removeItem_ShouldDeleteAndReturnCart_WhenItemFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(cartItemRepository.findByIdAndCartId(cartItemId, cartId)).thenReturn(Optional.of(sampleCartItem));

            CartResponseDto response = cartService.removeItem(customerId, cartItemId);

            verify(cartItemRepository).delete(sampleCartItem);
            assertThat(response.getCartId()).isEqualTo(cartId);
        }
    }

    @Nested
    @DisplayName("clearCart() Tests")
    class ClearCartTests {

        @Test
        @DisplayName("Should delegate to deleteByCartId using resolved cart ID")
        void clearCart_ShouldCallDeleteByCartId() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));

            cartService.clearCart(customerId);

            verify(cartItemRepository).deleteByCartId(cartId);
        }
    }

    @Nested
    @DisplayName("toItemResponseDto() Tests")
    class ToItemResponseDtoTests {

        @Test
        @DisplayName("Should accurately map fields and multiply price by quantity for subtotal")
        void toItemResponseDto_ShouldMapVariantDetailsAndSubtotal() {
            sampleCartItem.setQuantity(4);

            CartItemResponseDto dto = cartService.toItemResponseDto(sampleCartItem);

            assertThat(dto.getCartItemId()).isEqualTo(cartItemId);
            assertThat(dto.getProductVariantId()).isEqualTo(variantId);
            assertThat(dto.getProductName()).isEqualTo("Tailored Linen Blazer");
            assertThat(dto.getSize()).isEqualTo("L");
            assertThat(dto.getColour()).isEqualTo("Navy");
            assertThat(dto.getPrice()).isEqualByComparingTo("120.00");
            assertThat(dto.getQuantity()).isEqualTo(4);
            assertThat(dto.getSubtotal()).isEqualByComparingTo("480.00"); // 120.00 * 4
        }
    }
}