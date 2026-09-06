package com.ecommerce.VougeVault.delivery.repository;

import com.ecommerce.VougeVault.delivery.entity.DeliveryPerson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Long> {
    Optional<DeliveryPerson> findByUserId(Long userId);

    List<DeliveryPerson> findByIsAvailableTrue();

    @Query(value = "SELECT dp.*, COUNT(d.id) as active_count " +
            "FROM delivery_persons dp " +
            "LEFT JOIN deliveries d ON dp.id = d.delivery_person_id " +
            "AND d.status NOT IN ('DELIVERED', 'FAILED') " +
            "WHERE dp.is_available = true " +
            "GROUP BY dp.id " +
            "ORDER BY active_count ASC " +
            "LIMIT 1", nativeQuery = true)
    Optional<DeliveryPerson> findLeastBusyAvailableDeliveryPerson();
}
