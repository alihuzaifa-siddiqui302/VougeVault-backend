package com.ecommerce.VougeVault.wishlist.service;

import com.ecommerce.VougeVault.catalog.Repository.ProductVariantRepository;
import com.ecommerce.VougeVault.catalog.entity.Product;
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
class WishlistServiceTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @InjectMocks
    private WishlistService wishlistService;

    private User sampleCustomer;
    private Product sampleProduct;
    private ProductVariant sampleVariant;
    private Wishlist sampleWishlist;
    private WishlistItem sampleItem;
    private AddToWishlistDto addToWishlistDto;

    private final Long customerId = 1L;
    private final Long variantId = 100L;
    private final Long wishlistId = 10L;
    private final Long wishlistItemId = 500L;

    @BeforeEach
    void setUp() {
        sampleCustomer = new User();
        sampleCustomer.setId(customerId);
        sampleCustomer.setEmail("customer@example.com");

        sampleProduct = new Product();
        sampleProduct.setId(50L);
        sampleProduct.setName("Silk Blouse");

        sampleVariant = new ProductVariant();
        sampleVariant.setId(variantId);
        sampleVariant.setSize("M");
        sampleVariant.setColor("Emerald");
        sampleVariant.setPrice(new BigDecimal("79.99"));
        sampleVariant.setProduct(sampleProduct);

        sampleWishlist = new Wishlist();
        sampleWishlist.setId(wishlistId);
        sampleWishlist.setCustomer(sampleCustomer);
        sampleWishlist.setItems(new ArrayList<>());

        sampleItem = new WishlistItem();
        sampleItem.setId(wishlistItemId);
        sampleItem.setWishlist(sampleWishlist);
        sampleItem.setProductVariant(sampleVariant);

        addToWishlistDto = new AddToWishlistDto();
        addToWishlistDto.setProductVariantId(variantId);
    }

    @Nested
    @DisplayName("getOrCreateWishlist() Tests")
    class GetOrCreateWishlistTests {

        @Test
        @DisplayName("Should return existing wishlist when found for customer")
        void getOrCreateWishlist_ShouldReturnExisting_WhenWishlistFound() {
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleWishlist));

            Wishlist result = wishlistService.getOrCreateWishlist(customerId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(wishlistId);
            verify(userRepository, never()).findById(anyLong());
            verify(wishlistRepository, never()).save(any(Wishlist.class));
        }

        @Test
        @DisplayName("Should throw RuntimeException when wishlist does not exist and customer is not found")
        void getOrCreateWishlist_ShouldThrowException_WhenCustomerNotFound() {
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
            when(userRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> wishlistService.getOrCreateWishlist(customerId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("customer not found");

            verify(wishlistRepository, never()).save(any(Wishlist.class));
        }

        @Test
        @DisplayName("Should create and save a new wishlist when not found but customer exists")
        void getOrCreateWishlist_ShouldCreateAndSave_WhenCustomerFound() {
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
            when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(invocation -> {
                Wishlist w = invocation.getArgument(0);
                w.setId(wishlistId);
                return w;
            });

            Wishlist result = wishlistService.getOrCreateWishlist(customerId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(wishlistId);
            assertThat(result.getCustomer()).isEqualTo(sampleCustomer);
            verify(wishlistRepository).save(any(Wishlist.class));
        }
    }

    @Nested
    @DisplayName("addToWishlist() Tests")
    class AddToWishlistTests {

        @Test
        @DisplayName("Should throw RuntimeException when product variant does not exist")
        void addToWishlist_ShouldThrowException_WhenVariantNotFound() {
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleWishlist));
            when(variantRepository.findById(variantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> wishlistService.addToWishlist(customerId, addToWishlistDto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("product variant not found");

            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
        }

        @Test
        @DisplayName("Should save new wishlistItem when item is not already in wishlist")
        void addToWishlist_ShouldSaveItem_WhenItemNotAlreadyPresent() {
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleWishlist));
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(wishlistItemRepository.findByWishlistIdAndProductVariantId(wishlistId, variantId))
                    .thenReturn(Optional.empty());

            WishlistResponseDto response = wishlistService.addToWishlist(customerId, addToWishlistDto);

            ArgumentCaptor<WishlistItem> itemCaptor = ArgumentCaptor.forClass(WishlistItem.class);
            verify(wishlistItemRepository).save(itemCaptor.capture());

            WishlistItem savedItem = itemCaptor.getValue();
            assertThat(savedItem.getWishlist()).isEqualTo(sampleWishlist);
            assertThat(savedItem.getProductVariant()).isEqualTo(sampleVariant);
            assertThat(response.getId()).isEqualTo(wishlistId);
        }

        @Test
        @DisplayName("Should skip saving duplicate item when already present in wishlist")
        void addToWishlist_ShouldNotSaveItem_WhenItemAlreadyPresent() {
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleWishlist));
            when(variantRepository.findById(variantId)).thenReturn(Optional.of(sampleVariant));
            when(wishlistItemRepository.findByWishlistIdAndProductVariantId(wishlistId, variantId))
                    .thenReturn(Optional.of(sampleItem));

            WishlistResponseDto response = wishlistService.addToWishlist(customerId, addToWishlistDto);

            verify(wishlistItemRepository, never()).save(any(WishlistItem.class));
            assertThat(response.getId()).isEqualTo(wishlistId);
        }
    }

    @Nested
    @DisplayName("getWishlist() Tests")
    class GetWishlistTests {

        @Test
        @DisplayName("Should return mapped WishlistResponseDto with items")
        void getWishlist_ShouldReturnMappedDtoWithItems() {
            sampleWishlist.setItems(List.of(sampleItem));
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleWishlist));

            WishlistResponseDto response = wishlistService.getWishlist(customerId);

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(wishlistId);
            assertThat(response.getItemDtos()).hasSize(1);

            WishlistItemResponseDto itemDto = response.getItemDtos().get(0);
            assertThat(itemDto.getWishlistItemId()).isEqualTo(wishlistItemId);
            assertThat(itemDto.getProductVariantId()).isEqualTo(variantId);
            assertThat(itemDto.getProductName()).isEqualTo("Silk Blouse");
            assertThat(itemDto.getSize()).isEqualTo("M");
            assertThat(itemDto.getColour()).isEqualTo("Emerald");
            assertThat(itemDto.getPRICE()).isEqualByComparingTo("79.99");
        }

        @Test
        @DisplayName("Should return empty item list when wishlist has no items")
        void getWishlist_ShouldReturnEmptyItems_WhenWishlistIsEmpty() {
            sampleWishlist.setItems(Collections.emptyList());
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleWishlist));

            WishlistResponseDto response = wishlistService.getWishlist(customerId);

            assertThat(response.getId()).isEqualTo(wishlistId);
            assertThat(response.getItemDtos()).isEmpty();
        }
    }

    @Nested
    @DisplayName("removeFromWishlist() Tests")
    class RemoveFromWishlistTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when wishlistItem does not exist for wishlist")
        void removeFromWishlist_ShouldThrowException_WhenItemNotFound() {
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleWishlist));
            when(wishlistItemRepository.findByIdAndWishlistId(wishlistItemId, wishlistId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> wishlistService.removeFromWishlist(customerId, wishlistItemId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Wishlist item not found");

            verify(wishlistItemRepository, never()).delete(any(WishlistItem.class));
        }

        @Test
        @DisplayName("Should delete wishlistItem and return updated wishlist response")
        void removeFromWishlist_ShouldDeleteItem_WhenFound() {
            when(wishlistRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleWishlist));
            when(wishlistItemRepository.findByIdAndWishlistId(wishlistItemId, wishlistId))
                    .thenReturn(Optional.of(sampleItem));

            WishlistResponseDto response = wishlistService.removeFromWishlist(customerId, wishlistItemId);

            verify(wishlistItemRepository).delete(sampleItem);
            assertThat(response.getId()).isEqualTo(wishlistId);
        }
    }
}