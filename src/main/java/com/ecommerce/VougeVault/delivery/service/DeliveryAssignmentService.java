package com.ecommerce.VougeVault.delivery.service;

import com.ecommerce.VougeVault.brand.entity.Brand;
import com.ecommerce.VougeVault.delivery.entity.Delivery;
import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import com.ecommerce.VougeVault.delivery.entity.DeliveryStatus;
import com.ecommerce.VougeVault.delivery.repository.DeliveryPersonRepository;
import com.ecommerce.VougeVault.delivery.repository.DeliveryRepository;
import com.ecommerce.VougeVault.order.entity.Order;
import com.ecommerce.VougeVault.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAssignmentService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;

    @Transactional
    public void assignDeliveryForOrder(Order order) {
        // Get brand from the first order item (all items belong to same brand, enforced by order creation)
        Brand brand = order.getItems().get(0).getProductVariant().getProduct().getBrand();

        String pickupAddress = buildBrandAddress(brand);
        String dropAddress = order.getDeliveryAddress();

        Delivery delivery = new Delivery();
        delivery.setOrder(order);
        delivery.setPickupAddress(pickupAddress);
        delivery.setDropAddress(dropAddress);
        delivery.setStatus(DeliveryStatus.ASSIGNED);

        // Try to assign to least-busy available delivery person
        Optional<DeliveryPerson> availableDP = deliveryPersonRepository.findLeastBusyAvailableDeliveryPerson();

        if (availableDP.isPresent()) {
            delivery.setDeliveryPerson(availableDP.get());
            log.info("Assigned order {} to delivery person {}", order.getId(), availableDP.get().getUser().getName());
        } else {
            log.warn("No available delivery person for order {}, leaving unassigned", order.getId());
        }

        deliveryRepository.save(delivery);
    }

    private String buildBrandAddress(Brand brand) {
        return String.format("%s, %s, %s - %s",
                brand.getAddress(),
                brand.getCity(),
                brand.getState(),
                brand.getPincode()
        );
    }
}
