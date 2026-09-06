package com.ecommerce.VougeVault.order.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.cart.entity.Cart;
import com.ecommerce.VougeVault.cart.entity.CartItem;
import com.ecommerce.VougeVault.cart.repository.CartItemRepository;
import com.ecommerce.VougeVault.cart.repository.CartRepository;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final OrderStateMachine orderStateMachine;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderResponseDto checkout(Long customerId, CheckoutRequestDto dto) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        List<CartItem> cartItems = cart.getItems();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout with an empty cart");
        }
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        Order order = new Order();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setDeliveryAddress(dto.getDeliveryAddress());
        order.setDeliveryCity(dto.getDeliveryCity());
        order.setDeliveryPhone(dto.getDeliveryPhone());
        order.setDeliveryLatitude(dto.getDeliveryLatitude());
        order.setDeliveryLongitude(dto.getDeliveryLongitude());
        order = orderRepository.save(order);

        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : cartItems) {
            ProductVariant variant = item.getProductVariant();
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductVariant(variant);
            Brand brand = new Brand();
            brand.setId(variant.getProduct().getBrand().getId());
            orderItem.setBrand(brand);

            orderItem.setQuantity(item.getQuantity());
            orderItem.setPriceAtPurchase(variant.getPrice());
            orderItem.setProductNameSnapshot(variant.getProduct().getName());
            orderItem.setSizeSnapshot(variant.getSize());
            orderItem.setColorSnapshot(variant.getColor());

            orderItemRepository.save(orderItem);
            total = total.add(variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));

        }
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        cartItemRepository.deleteByCartId(cart.getId());

        // After saving order and clearing cart, publish event
        eventPublisher.publishEvent(new OrderCreatedEvent(this, order));

        return toResponseDto(order);
    }

    public List<OrderResponseDto> getMyOrders(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    public OrderResponseDto getMyOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toResponseDto(order);
    }

    @Transactional
    public OrderResponseDto cancelOrder(Long orderId, Long customerId) {
        Order order = orderRepository.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!orderStateMachine.canCancel(order.getStatus())) {
            throw new IllegalStateException("Order cannot be cancelled in its current status: " + order.getStatus());
        }

        orderStateMachine.transition(order, OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toResponseDto(order);
    }


    private OrderResponseDto toResponseDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> new OrderItemResponseDto(
                        item.getProductNameSnapshot(),
                        item.getSizeSnapshot(),
                        item.getColorSnapshot(),
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .collect(Collectors.toList());

        return new OrderResponseDto(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemDtos
        );
    }
}
