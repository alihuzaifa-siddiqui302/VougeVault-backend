package com.ecommerce.VougeVault.order.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.cart.entity.Cart;
import com.ecommerce.VougeVault.cart.entity.CartItem;
import com.ecommerce.VougeVault.cart.repository.CartItemRepository;
import com.ecommerce.VougeVault.cart.repository.CartRepository;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.order.dto.CheckoutRequestDto;
import com.ecommerce.VougeVault.order.dto.OrderItemResponseDto;
import com.ecommerce.VougeVault.order.dto.OrderResponseDto;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import com.ecommerce.VougeVault.order.entity.OrderStatus;
import com.ecommerce.VougeVault.order.event.OrderCreatedEvent;
import com.ecommerce.VougeVault.order.repository.OrderItemRepository;
import com.ecommerce.VougeVault.order.repository.OrderRepository;
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
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderStateMachine orderStateMachine;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User sampleCustomer;
    private Brand sampleBrand;
    private Product sampleProduct;
    private ProductVariant sampleVariant;
    private Cart sampleCart;
    private CartItem sampleCartItem;
    private Order sampleOrder;
    private OrderItem sampleOrderItem;
    private CheckoutRequestDto checkoutRequestDto;

    private final Long customerId = 1L;
    private final Long cartId = 10L;
    private final Long orderId = 100L;
    private final Long brandId = 5L;
    private final Long variantId = 50L;

    @BeforeEach
    void setUp() {
        sampleCustomer = new User();
        sampleCustomer.setId(customerId);
        sampleCustomer.setEmail("customer@example.com");

        sampleBrand = new Brand();
        sampleBrand.setId(brandId);
        sampleBrand.setName("Vouge Apparel");

        sampleProduct = new Product();
        sampleProduct.setId(20L);
        sampleProduct.setName("Silk Shirt");
        sampleProduct.setBrand(sampleBrand);

        sampleVariant = new ProductVariant();
        sampleVariant.setId(variantId);
        sampleVariant.setSize("M");
        sampleVariant.setColor("Black");
        sampleVariant.setPrice(new BigDecimal("50.00"));
        sampleVariant.setProduct(sampleProduct);

        sampleCartItem = new CartItem();
        sampleCartItem.setId(200L);
        sampleCartItem.setProductVariant(sampleVariant);
        sampleCartItem.setQuantity(2);

        sampleCart = new Cart();
        sampleCart.setId(cartId);
        sampleCart.setCustomer(sampleCustomer);
        sampleCart.setItems(new ArrayList<>(List.of(sampleCartItem)));

        sampleOrderItem = new OrderItem();
        sampleOrderItem.setId(300L);
        sampleOrderItem.setProductVariant(sampleVariant);
        sampleOrderItem.setBrand(sampleBrand);
        sampleOrderItem.setQuantity(2);
        sampleOrderItem.setPriceAtPurchase(new BigDecimal("50.00"));
        sampleOrderItem.setProductNameSnapshot("Silk Shirt");
        sampleOrderItem.setSizeSnapshot("M");
        sampleOrderItem.setColorSnapshot("Black");

        sampleOrder = new Order();
        sampleOrder.setId(orderId);
        sampleOrder.setCustomer(sampleCustomer);
        sampleOrder.setStatus(OrderStatus.CREATED);
        sampleOrder.setTotalAmount(new BigDecimal("100.00"));
        sampleOrder.setDeliveryAddress("123 High Street");
        sampleOrder.setDeliveryCity("London");
        sampleOrder.setDeliveryPhone("9876543210");
        sampleOrder.setCreatedAt(LocalDateTime.now());
        sampleOrder.setItems(new ArrayList<>(List.of(sampleOrderItem)));

        checkoutRequestDto = new CheckoutRequestDto();
        checkoutRequestDto.setDeliveryAddress("123 High Street");
        checkoutRequestDto.setDeliveryCity("London");
        checkoutRequestDto.setDeliveryPhone("9876543210");
        checkoutRequestDto.setDeliveryLatitude(BigDecimal.valueOf(51.5074));
        checkoutRequestDto.setDeliveryLongitude(BigDecimal.valueOf(-0.1278));
    }

    @Nested
    @DisplayName("checkout() Tests")
    class CheckoutTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when cart does not exist")
        void checkout_ShouldThrowException_WhenCartNotFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.checkout(customerId, checkoutRequestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Cart not found");

            verifyNoInteractions(userRepository, orderRepository, orderItemRepository, cartItemRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when cart has null or empty items")
        void checkout_ShouldThrowException_WhenCartIsEmpty() {
            sampleCart.setItems(Collections.emptyList());
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));

            assertThatThrownBy(() -> orderService.checkout(customerId, checkoutRequestDto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Cannot checkout with an empty cart");

            verifyNoInteractions(userRepository, orderRepository, orderItemRepository, cartItemRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when customer does not exist")
        void checkout_ShouldThrowException_WhenCustomerNotFound() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(userRepository.findById(customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.checkout(customerId, checkoutRequestDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Customer not found");

            verifyNoInteractions(orderRepository, orderItemRepository, cartItemRepository, eventPublisher);
        }

        @Test
        @DisplayName("Should create order, save order items, delete cart items, and publish OrderCreatedEvent")
        void checkout_ShouldCompleteSuccessfully_WhenValid() {
            when(cartRepository.findByCustomerId(customerId)).thenReturn(Optional.of(sampleCart));
            when(userRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                if (o.getId() == null) {
                    o.setId(orderId);
                }
                if (o.getItems() == null) {
                    o.setItems(new ArrayList<>());
                }
                return o;
            });

            OrderResponseDto response = orderService.checkout(customerId, checkoutRequestDto);

            // Verify order initial & final persistence
            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository, times(2)).save(orderCaptor.capture());
            Order finalSavedOrder = orderCaptor.getValue();

            assertThat(finalSavedOrder.getCustomer()).isEqualTo(sampleCustomer);
            assertThat(finalSavedOrder.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(finalSavedOrder.getDeliveryAddress()).isEqualTo("123 High Street");
            assertThat(finalSavedOrder.getDeliveryCity()).isEqualTo("London");
            assertThat(finalSavedOrder.getTotalAmount()).isEqualByComparingTo("100.00"); // 50.00 * 2

            // Verify order item persistence with snapshots
            ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
            verify(orderItemRepository).save(itemCaptor.capture());
            OrderItem savedItem = itemCaptor.getValue();

            assertThat(savedItem.getProductVariant()).isEqualTo(sampleVariant);
            assertThat(savedItem.getBrand().getId()).isEqualTo(brandId);
            assertThat(savedItem.getQuantity()).isEqualTo(2);
            assertThat(savedItem.getPriceAtPurchase()).isEqualByComparingTo("50.00");
            assertThat(savedItem.getProductNameSnapshot()).isEqualTo("Silk Shirt");
            assertThat(savedItem.getSizeSnapshot()).isEqualTo("M");
            assertThat(savedItem.getColorSnapshot()).isEqualTo("Black");

            // Verify cart cleanup and event publishing
            verify(cartItemRepository).deleteByCartId(cartId);
            verify(eventPublisher).publishEvent(any(OrderCreatedEvent.class));
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(orderId);
        }
    }

    @Nested
    @DisplayName("getMyOrders() Tests")
    class GetMyOrdersTests {

        @Test
        @DisplayName("Should return list of mapped OrderResponseDtos")
        void getMyOrders_ShouldReturnMappedOrders() {
            when(orderRepository.findByCustomerId(customerId)).thenReturn(List.of(sampleOrder));

            List<OrderResponseDto> result = orderService.getMyOrders(customerId);

            assertThat(result).hasSize(1);
            OrderResponseDto dto = result.get(0);
            assertThat(dto.getOrderId()).isEqualTo(orderId);
            assertThat(dto.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(dto.getTotalAmount()).isEqualByComparingTo("100.00");
            assertThat(dto.getItems()).hasSize(1);

            OrderItemResponseDto itemDto = dto.getItems().get(0);
            assertThat(itemDto.getProductName()).isEqualTo("Silk Shirt");
            assertThat(itemDto.getSize()).isEqualTo("M");
            assertThat(itemDto.getColor()).isEqualTo("Black");
            assertThat(itemDto.getQuantity()).isEqualTo(2);
            assertThat(itemDto.getPriceAtPurchase()).isEqualByComparingTo("50.00");
            assertThat(itemDto.getSubtotal()).isEqualByComparingTo("100.00"); // 50.00 * 2
        }

        @Test
        @DisplayName("Should return empty list when customer has no orders")
        void getMyOrders_ShouldReturnEmptyList_WhenNoOrders() {
            when(orderRepository.findByCustomerId(customerId)).thenReturn(Collections.emptyList());

            List<OrderResponseDto> result = orderService.getMyOrders(customerId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMyOrder() Tests")
    class GetMyOrderTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order does not exist for customer")
        void getMyOrder_ShouldThrowException_WhenOrderNotFound() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getMyOrder(orderId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found");
        }

        @Test
        @DisplayName("Should return mapped OrderResponseDto when order exists")
        void getMyOrder_ShouldReturnOrder_WhenFound() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));

            OrderResponseDto result = orderService.getMyOrder(orderId, customerId);

            assertThat(result).isNotNull();
            assertThat(result.getOrderId()).isEqualTo(orderId);
            assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
            assertThat(result.getTotalAmount()).isEqualByComparingTo("100.00");
        }
    }

    @Nested
    @DisplayName("cancelOrder() Tests")
    class CancelOrderTests {

        @Test
        @DisplayName("Should throw ResourceNotFoundException when order does not exist")
        void cancelOrder_ShouldThrowException_WhenOrderNotFound() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found");

            verifyNoInteractions(orderStateMachine);
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should throw IllegalStateException when state machine disallows cancellation")
        void cancelOrder_ShouldThrowException_WhenCannotCancel() {
            sampleOrder.setStatus(OrderStatus.DELIVERED);
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));
            when(orderStateMachine.canCancel(OrderStatus.DELIVERED)).thenReturn(false);

            assertThatThrownBy(() -> orderService.cancelOrder(orderId, customerId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Order cannot be cancelled in its current status: DELIVERED");

            verify(orderStateMachine, never()).transition(any(), any());
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Should transition order to CANCELLED and save when cancellation is valid")
        void cancelOrder_ShouldCancelAndSave_WhenAllowed() {
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(sampleOrder));
            when(orderStateMachine.canCancel(OrderStatus.CREATED)).thenReturn(true);
            doAnswer(invocation -> {
                Order o = invocation.getArgument(0);
                o.setStatus(OrderStatus.CANCELLED);
                return null;
            }).when(orderStateMachine).transition(sampleOrder, OrderStatus.CANCELLED);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

            OrderResponseDto response = orderService.cancelOrder(orderId, customerId);

            verify(orderStateMachine).transition(sampleOrder, OrderStatus.CANCELLED);
            verify(orderRepository).save(sampleOrder);
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}