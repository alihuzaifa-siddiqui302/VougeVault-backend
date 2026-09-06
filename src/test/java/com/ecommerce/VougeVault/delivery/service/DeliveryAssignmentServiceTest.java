package com.ecommerce.VougeVault.delivery.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.catalog.entity.Product;
import com.ecommerce.VougeVault.catalog.entity.ProductVariant;
import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import com.ecommerce.VougeVault.delivery.repository.DeliveryPersonRepository;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import com.ecommerce.VougeVault.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryAssignmentServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private DeliveryPersonRepository deliveryPersonRepository;

    @InjectMocks
    private DeliveryAssignmentService deliveryAssignmentService;

    private Order sampleOrder;
    private Brand sampleBrand;
    private DeliveryPerson sampleDeliveryPerson;

    private final Long orderId = 200L;
    private final String expectedPickupAddress = "123 Fashion Street, Mumbai, Maharashtra - 400001";
    private final String dropAddress = "Flat 502, Skyline Towers, Andheri West";

    @BeforeEach
    void setUp() {
        sampleBrand = new Brand();
        sampleBrand.setId(10L);
        sampleBrand.setName("Vouge Apparel");
        sampleBrand.setAddress("123 Fashion Street");
        sampleBrand.setCity("Mumbai");
        sampleBrand.setState("Maharashtra");
        sampleBrand.setPincode("400001");

        Product product = new Product();
        product.setId(50L);
        product.setBrand(sampleBrand);

        ProductVariant variant = new ProductVariant();
        variant.setId(500L);
        variant.setProduct(product);

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1000L);
        orderItem.setProductVariant(variant);

        sampleOrder = new Order();
        sampleOrder.setId(orderId);
        sampleOrder.setDeliveryAddress(dropAddress);
        sampleOrder.setItems(List.of(orderItem));

        User deliveryUser = new User();
        deliveryUser.setId(1L);
        deliveryUser.setName("Alex Courier");

        sampleDeliveryPerson = new DeliveryPerson();
        sampleDeliveryPerson.setId(99L);
        sampleDeliveryPerson.setUser(deliveryUser);
    }

    @Nested
    @DisplayName("assignDeliveryForOrder() Tests")
    class AssignDeliveryTests {

        @Test
        @DisplayName("Should assign least-busy delivery person, format pickup address, and persist delivery with ASSIGNED status")
        void assignDeliveryForOrder_ShouldAssignDeliveryPerson_WhenAvailable() {
            when(deliveryPersonRepository.findLeastBusyAvailableDeliveryPerson())
                    .thenReturn(Optional.of(sampleDeliveryPerson));

            deliveryAssignmentService.assignDeliveryForOrder(sampleOrder);

            ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
            verify(deliveryRepository).save(deliveryCaptor.capture());
            Delivery savedDelivery = deliveryCaptor.getValue();

            assertThat(savedDelivery).isNotNull();
            assertThat(savedDelivery.getOrder()).isEqualTo(sampleOrder);
            assertThat(savedDelivery.getPickupAddress()).isEqualTo(expectedPickupAddress);
            assertThat(savedDelivery.getDropAddress()).isEqualTo(dropAddress);
            assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
            assertThat(savedDelivery.getDeliveryPerson()).isEqualTo(sampleDeliveryPerson);
        }

        @Test
        @DisplayName("Should save unassigned delivery with ASSIGNED status when no delivery person is available")
        void assignDeliveryForOrder_ShouldSaveUnassignedDelivery_WhenNoDeliveryPersonAvailable() {
            when(deliveryPersonRepository.findLeastBusyAvailableDeliveryPerson())
                    .thenReturn(Optional.empty());

            deliveryAssignmentService.assignDeliveryForOrder(sampleOrder);

            ArgumentCaptor<Delivery> deliveryCaptor = ArgumentCaptor.forClass(Delivery.class);
            verify(deliveryRepository).save(deliveryCaptor.capture());
            Delivery savedDelivery = deliveryCaptor.getValue();

            assertThat(savedDelivery).isNotNull();
            assertThat(savedDelivery.getOrder()).isEqualTo(sampleOrder);
            assertThat(savedDelivery.getPickupAddress()).isEqualTo(expectedPickupAddress);
            assertThat(savedDelivery.getDropAddress()).isEqualTo(dropAddress);
            assertThat(savedDelivery.getStatus()).isEqualTo(DeliveryStatus.ASSIGNED);
            assertThat(savedDelivery.getDeliveryPerson()).isNull();
        }
    }
}